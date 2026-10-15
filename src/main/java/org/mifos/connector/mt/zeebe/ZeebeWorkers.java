package org.mifos.connector.mt.zeebe;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.zeebe.client.ZeebeClient;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.mifos.connector.mt.client.AccountBalanceClient;
import org.mifos.connector.mt.client.CardAccountClient;
import org.mifos.connector.mt.client.CardNotificationPublisher;
import org.mifos.connector.mt.client.CoreBankingClient;
import org.mifos.connector.mt.client.MtBankClient;
import org.mifos.connector.mt.model.MtAuthorizationRequest;
import org.mifos.connector.mt.model.MtCaptureRequest;
import org.mifos.connector.mt.model.MtPaymentStatus;
import org.mifos.connector.mt.model.MtRefundRequest;
import org.mifos.connector.mt.model.MtVoidRequest;
import org.mifos.connector.mt.service.MtPaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(ZeebeClient.class)
public class ZeebeWorkers {
    private static final Logger log = LoggerFactory.getLogger(ZeebeWorkers.class);
    private final ZeebeClient zeebeClient;
    private final MtPaymentService service;
    private final CardAccountClient cards;
    private final MtBankClient bank;
    private final AccountBalanceClient balances;
    private final CardNotificationPublisher notifications;
    private final CoreBankingClient coreBanking;
    private final ObjectMapper json;
    @Value("${zeebe.client.evenly-allocated-max-jobs}")
    private int maxJobs;

    public ZeebeWorkers(ZeebeClient zeebeClient, MtPaymentService service, CardAccountClient cards, MtBankClient bank,
            AccountBalanceClient balances, CardNotificationPublisher notifications, CoreBankingClient coreBanking,
            ObjectMapper json) {
        this.zeebeClient = zeebeClient;
        this.service = service;
        this.cards = cards;
        this.bank = bank;
        this.balances = balances;
        this.notifications = notifications;
        this.coreBanking = coreBanking;
        this.json = json;
    }

    @PostConstruct
    public void setup() {
        worker("mtAuthorizePayment", vars -> service.authorize(mapAuth(vars)));
        worker("mtCapturePayment", vars -> {
            MtCaptureRequest req = new MtCaptureRequest();
            req.setAuthorizationId(str(vars.getOrDefault("authorizationId", vars.get("externalId"))));
            req.setAmount(toDecimal(vars.get("amount")));
            return service.capture(req);
        });
        worker("mtGetStatus", vars -> service.status(str(vars.getOrDefault("transactionId", vars.get("externalId")))));
        worker("mtRefundPayment", vars -> {
            MtRefundRequest req = new MtRefundRequest();
            req.setOriginalTransactionId(str(vars.getOrDefault("originalTransactionId", vars.get("externalId"))));
            req.setAmount(toDecimal(vars.get("amount")));
            req.setCurrency(str(vars.getOrDefault("currency", "TND")));
            req.setReason(str(vars.get("narrative")));
            return service.refund(req);
        });
        worker("mtVoidPayment", vars -> {
            MtVoidRequest req = new MtVoidRequest();
            req.setOriginalTransactionId(str(vars.getOrDefault("originalTransactionId", vars.get("externalId"))));
            req.setAuthorizationId(str(vars.get("authorizationId")));
            req.setReason(str(vars.get("narrative")));
            return service.voidTransaction(req);
        });
        worker("mtSettleInbound", vars -> service.authorize(mapAuth(vars)));
        sideEffect("mtFindAccountByPan", this::findAccount);
        sideEffect("mtCheckBalance", this::checkBalance);
        sideEffect("mtApplyUserDecision", this::applyUserDecision);
        sideEffect("mtRequestCardholderConfirmation", this::requestConfirmation);
        sideEffect("mtNotifyInsufficientBalance", this::notifyInsufficient);
        sideEffect("mtRecordCancelledDebit", this::recordCancelledDebit);
        sideEffect("mtClaimBlock", this::claimBlock);
        sideEffect("mtMarkFundsBlocked", this::markFundsBlocked);
        sideEffect("mtExpireConfirmation", this::expireConfirmation);
        sideEffect("mtCreditCardholder", this::creditCardholder);
        sideEffect("mtReplyToNetwork", this::replyToNetwork);
        log.info("Registered Monétique Tunisie Zeebe workers");
    }

    private void sideEffect(String type, java.util.function.Consumer<Map<String, Object>> fn) {
        zeebeClient.newWorker().jobType(type).handler((client, job) -> {
            log.info("Job '{}' process='{}' key={}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = new HashMap<>(job.getVariablesAsMap());
            try {
                fn.accept(variables);
            } catch (RuntimeException ex) {
                log.warn("MT job '{}' failed: {}", type, ex.getMessage());
                variables.put("transactionFailed", true);
                variables.put("transferCreateFailed", true);
                variables.put("transferCode", "05");
                variables.put("errorMessage", ex.getMessage());
            }
            client.newCompleteCommand(job.getKey()).variables(variables).send().join();
        }).name(type).maxJobsActive(maxJobs).open();
    }

    private void findAccount(Map<String, Object> vars) {
        String pan = str(vars.get("pan") != null ? vars.get("pan") : vars.get("panToken"));
        String entryMode = str(vars.get("entryMode"));
        Map<String, Object> card = cards.findByPan(pan, entryMode);
        if (card == null || card.get("accountId") == null || String.valueOf(card.get("accountId")).isBlank()) {
            vars.put("partyLookupFailed", true);
            vars.put("transactionFailed", true);
            vars.put("transferCode", "14");
            return;
        }
        String accountId = String.valueOf(card.get("accountId"));
        vars.put("partyLookupFailed", false);
        vars.put("accountId", accountId);
        vars.put("userId", card.get("userId"));
        vars.put("partyId", accountId);
        vars.put("partyIdType", "ACCOUNT_ID");
        vars.put("cardId", card.get("cardId"));
        vars.put("last4", card.get("last4"));
        vars.put("cardTenantId", card.get("tenantId"));
        if (card.get("confirmationRequired") != null) {
            vars.put("confirmationRequired", truth(card.get("confirmationRequired")));
        }
        rewritePayer(vars, accountId, str(card.get("holderName")));
    }

    private void checkBalance(Map<String, Object> vars) {
        BigDecimal amount = toDecimal(vars.get("amount"));
        BigDecimal available = balances.available(str(vars.get("accountId")), str(vars.get("tenantId")));
        boolean known = available != null;
        boolean sufficient = !known || available.compareTo(amount) >= 0;
        vars.put("balanceUnavailable", !known);
        vars.put("balanceSufficient", sufficient);
        vars.put("availableBalance", known ? available.toPlainString() : "");
    }

    private void applyUserDecision(Map<String, Object> vars) {
        boolean confirmed = truth(vars.get("userConfirmed"));
        vars.put("userConfirmed", confirmed);
        vars.put("cardholderRejected", !confirmed);
        vars.put("transactionFailed", !confirmed);
        if (!confirmed) {
            vars.put("transferCode", "17");
            vars.put("operationState", "CANCELLED");
        }
    }

    private void requestConfirmation(Map<String, Object> vars) {
        Map<String, Object> body = confirmation(vars);
        body.put("operationState", "CONFIRMATION_REQUIRED");
        bank.openConfirmation(body);
        notifications.publish(body, "CONFIRM");
        vars.put("confirmationRequested", true);
        vars.put("operationState", "CONFIRMATION_REQUIRED");
    }

    private void notifyInsufficient(Map<String, Object> vars) {
        Map<String, Object> body = confirmation(vars);
        body.put("operationState", "INSUFFICIENT_BALANCE");
        bank.openConfirmation(body);
        notifications.publish(body, "INSUFFICIENT_BALANCE");
        vars.put("transactionFailed", true);
        vars.put("transferCode", "51");
        vars.put("operationState", "INSUFFICIENT_BALANCE");
    }

    private void recordCancelledDebit(Map<String, Object> vars) {
        try {
            Map<String, Object> body = confirmation(vars);
            body.put("reference", vars.getOrDefault("clientReference", vars.get("transactionId")));
            body.put("cardId", vars.get("cardId"));
            body.put("last4", vars.get("last4"));
            String tenant = str(vars.get("cardTenantId"));
            coreBanking.recordRejectedDebit(body, tenant == null || tenant.isBlank() ? "tn" : tenant);
            vars.put("cancelledDebitRecorded", true);
        } catch (RuntimeException ex) {
            log.warn("Cancelled debit was not written to core banking: {}", ex.getMessage());
            vars.put("cancelledDebitRecorded", false);
        }
    }

    private void claimBlock(Map<String, Object> vars) {
        Map<String, Object> claim = bank.claimBlock(confirmation(vars));
        boolean claimed = claim != null && truth(claim.get("claimed"));
        vars.put("blockClaimed", claimed);
        if (!claimed) {
            vars.put("operationState", str(claim == null ? null : claim.get("operationState")));
        }
    }

    private void markFundsBlocked(Map<String, Object> vars) {
        Map<String, Object> body = confirmation(vars);
        body.put("operationState", "FUNDS_BLOCKED");
        try {
            bank.markBlocked(body);
            notifications.publish(body, "FUNDS_BLOCKED");
        } catch (RuntimeException ex) {
            log.warn("Blocked funds were not published: {}", ex.getMessage());
        }
        vars.put("operationState", "FUNDS_BLOCKED");
    }

    private void expireConfirmation(Map<String, Object> vars) {
        vars.put("cardholderRejected", true);
        vars.put("cardholderDecision", "EXPIRED");
        vars.put("transactionFailed", true);
        String transactionId = str(vars.get("transactionId"));
        if (transactionId != null) {
            bank.expireConfirmation(transactionId);
        }
    }

    private void creditCardholder(Map<String, Object> vars) {
        boolean failed = Boolean.TRUE.equals(vars.get("partyLookupFailed")) || vars.get("accountId") == null;
        vars.put("transferCreateFailed", failed);
        vars.put("transactionFailed", failed);
        if (!failed) {
            vars.put("transferCode", "00");
        }
    }

    private void replyToNetwork(Map<String, Object> vars) {
        boolean failed = truth(vars.get("transactionFailed")) || truth(vars.get("partyLookupFailed"))
                || truth(vars.get("cardholderRejected")) || truth(vars.get("transferPrepareFailed"))
                || truth(vars.get("transferCreateFailed")) || truth(vars.get("transferReleaseFailed"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clientReference", vars.get("clientReference"));
        body.put("outcome", failed ? "REJECTED" : "SETTLED");
        body.put("reasonCode", failed ? str(vars.getOrDefault("transferCode", "05")) : "ACSC");
        body.put("networkReference", vars.get("transactionId"));
        bank.reply(body);
        vars.put("networkReplied", true);
    }

    private Map<String, Object> confirmation(Map<String, Object> vars) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transactionId", vars.get("transactionId"));
        body.put("clientReference", vars.get("clientReference"));
        body.put("userId", vars.get("userId"));
        body.put("tenantId", vars.get("tenantId"));
        body.put("accountId", vars.get("accountId"));
        body.put("amount", vars.get("amount"));
        body.put("currency", vars.getOrDefault("currency", "TND"));
        body.put("merchant", vars.get("merchantId"));
        body.put("clientId", vars.get("userId"));
        body.put("availableBalance", vars.get("availableBalance"));
        return body;
    }

    @SuppressWarnings("unchecked")
    private void rewritePayer(Map<String, Object> vars, String accountId, String holderName) {
        Object raw = vars.get("channelRequest");
        if (!(raw instanceof String text) || text.isBlank()) {
            return;
        }
        try {
            Map<String, Object> request = json.readValue(text, Map.class);
            Object payer = request.get("payer");
            if (payer instanceof Map<?, ?> payerMap) {
                Map<String, Object> next = new LinkedHashMap<>((Map<String, Object>) payerMap);
                if (holderName != null) {
                    next.put("name", holderName);
                }
                Object info = next.get("partyIdInfo");
                Map<String, Object> party = info instanceof Map<?, ?> infoMap
                        ? new LinkedHashMap<>((Map<String, Object>) infoMap) : new LinkedHashMap<>();
                party.put("partyIdType", "ACCOUNT_ID");
                party.put("partyIdentifier", accountId);
                next.put("partyIdInfo", party);
                request.put("payer", next);
            }
            vars.put("channelRequest", json.writeValueAsString(request));
        } catch (Exception ex) {
            log.warn("Could not attach the account to the channel request: {}", ex.getMessage());
        }
    }

    private static boolean truth(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(str(value));
    }

    private void worker(String type, java.util.function.Function<Map<String, Object>, MtPaymentStatus> fn) {
        zeebeClient.newWorker().jobType(type).handler((client, job) -> {
            log.info("Job '{}' process='{}' key={}", job.getType(), job.getBpmnProcessId(), job.getKey());
            Map<String, Object> variables = new HashMap<>(job.getVariablesAsMap());
            try {
                apply(variables, fn.apply(variables));
            } catch (RuntimeException ex) {
                log.warn("MT job '{}' failed: {}", type, ex.getMessage());
                variables.put("transactionFailed", true);
                variables.put("transferSettlementFailed", true);
                variables.put("transferCode", "05");
                variables.put("errorMessage", ex.getMessage());
            }
            client.newCompleteCommand(job.getKey()).variables(variables).send().join();
        }).name(type).maxJobsActive(maxJobs).open();
    }

    private MtAuthorizationRequest mapAuth(Map<String, Object> vars) {
        MtAuthorizationRequest req = new MtAuthorizationRequest();
        req.setOrderId(str(vars.getOrDefault("transactionId", UUID.randomUUID().toString())));
        req.setAmount(toDecimal(vars.get("amount")));
        req.setCurrency(str(vars.getOrDefault("currency", "TND")));
        req.setPanToken(str(vars.getOrDefault("panToken", vars.get("creditorAccount"))));
        req.setMerchantId(str(vars.get("merchantId")));
        req.setTerminalId(str(vars.get("terminalId")));
        req.setCardholderName(str(vars.get("debtorName")));
        return req;
    }

    private void apply(Map<String, Object> vars, MtPaymentStatus status) {
        if (status == null) {
            vars.put("transactionFailed", true);
            vars.put("transferCode", "05");
            return;
        }
        boolean failed = "FAILED".equalsIgnoreCase(status.getStatus()) || !"00".equals(status.getResponseCode());
        vars.put("transactionFailed", failed);
        vars.put("transferSettlementFailed", failed);
        vars.put("authorizationId", status.getAuthorizationId());
        vars.put("externalId", status.getTransactionId());
        vars.put("transferCode", status.getResponseCode() != null ? status.getResponseCode() : (failed ? "05" : "00"));
        vars.put("railStatus", status.getStatus());
        vars.put("rail", "MT");
    }

    private static String str(Object v) { return v == null ? null : String.valueOf(v); }
    private static BigDecimal toDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(String.valueOf(v));
    }
}
