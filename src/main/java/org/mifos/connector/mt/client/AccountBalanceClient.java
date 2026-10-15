package org.mifos.connector.mt.client;

import java.math.BigDecimal;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Reads the available balance from the account connector. A failed lookup does not invent a balance. */
@Component
public class AccountBalanceClient {
    private static final Logger log = LoggerFactory.getLogger(AccountBalanceClient.class);
    private final RestClient http;

    public AccountBalanceClient(@Value("${mt.ams-base-url:http://ph-ee-connector-ams-mifos:70}") String baseUrl) {
        this.http = RestClient.builder().baseUrl(trim(baseUrl)).build();
    }

    public BigDecimal available(String accountId, String tenantId) {
        if (accountId == null || accountId.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> body = http.get()
                    .uri("/ams/accounts/ACCOUNT_ID/{id}/balance", accountId)
                    .header("Platform-TenantId", tenantId == null || tenantId.isBlank() ? "nexusbank" : tenantId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (body == null || body.get("availableBalance") == null) {
                return null;
            }
            String raw = String.valueOf(body.get("availableBalance")).trim();
            if (raw.isEmpty()) {
                return null;
            }
            return new BigDecimal(raw);
        } catch (RuntimeException ex) {
            log.warn("Account balance was not available for {}: {}", accountId, ex.getMessage());
            return null;
        }
    }

    private static String trim(String url) {
        if (url == null || url.isBlank()) {
            return "http://ph-ee-connector-ams-mifos:70";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
