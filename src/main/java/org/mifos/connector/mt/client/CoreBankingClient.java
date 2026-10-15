package org.mifos.connector.mt.client;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Writes the rejected card debit onto the customer's account in core banking. */
@Component
public class CoreBankingClient {
    private final RestClient http;

    public CoreBankingClient(@Value("${mt.core-banking-base-url:http://core-api-core-banking-api.api.svc.cluster.local}") String baseUrl) {
        this.http = RestClient.builder().baseUrl(trim(baseUrl)).build();
    }

    public void recordRejectedDebit(Map<String, Object> body, String tenantId) {
        http.post()
                .uri("/api/account/internal/rejected-debit")
                .header("Fineract-Platform-TenantId", tenantId)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private static String trim(String url) {
        if (url == null || url.isBlank()) {
            return "http://core-api-core-banking-api.api.svc.cluster.local";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
