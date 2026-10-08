package org.mifos.connector.mt.zeebe;

import io.camunda.zeebe.client.ZeebeClient;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
    @Value("${zeebe.client.evenly-allocated-max-jobs}")
    private int maxJobs;

    public ZeebeWorkers(ZeebeClient zeebeClient, MtPaymentService service) {
        this.zeebeClient = zeebeClient;
        this.service = service;
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
        log.info("Registered Monétique Tunisie Zeebe workers");
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
