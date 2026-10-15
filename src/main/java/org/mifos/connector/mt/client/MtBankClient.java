package org.mifos.connector.mt.client;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Calls mt-api, the bridge in front of the channel. */
@Component
public class MtBankClient {
    private static final Logger log = LoggerFactory.getLogger(MtBankClient.class);
    private final RestClient bank;
    private final RestClient notifications;

    public MtBankClient(@Value("${mt.gateway.base-url:http://localhost:8097}") String bankUrl,
            @Value("${mt.notification-api-base-url:http://localhost:8096}") String notificationUrl) {
        this.bank = RestClient.builder().baseUrl(trim(bankUrl)).build();
        this.notifications = RestClient.builder().baseUrl(trim(notificationUrl)).build();
    }

    public void openConfirmation(Map<String, Object> body) {
        bank.post().uri("/mt/v1/confirmations").body(body).retrieve().toBodilessEntity();
    }

    public void expireConfirmation(String transactionId) {
        bank.post().uri("/mt/v1/confirmations/{id}/expire", transactionId).retrieve().toBodilessEntity();
    }

    public Map<String, Object> claimBlock(Map<String, Object> body) {
        return bank.post().uri("/mt/v1/confirmations/claim").body(body).retrieve().body(new org.springframework.core.ParameterizedTypeReference<>() {});
    }

    public void markBlocked(Map<String, Object> body) {
        bank.post().uri("/mt/v1/confirmations/blocked").body(body).retrieve().toBodilessEntity();
    }

    public void reply(Map<String, Object> body) {
        bank.post().uri("/mt/v1/callbacks").body(body).retrieve().toBodilessEntity();
    }

    public void notifyCardholder(Map<String, Object> body) {
        try {
            notifications.post().uri("/api/notifications/payment-confirmation").body(body).retrieve().toBodilessEntity();
        } catch (RuntimeException ex) {
            log.warn("Payment confirmation notification was not delivered: {}", ex.getMessage());
        }
    }

    private static String trim(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
