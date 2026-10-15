package org.mifos.connector.mt.client;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Resolves the bank account behind a card PAN. The clear PAN is not stored by this connector. */
@Component
public class CardAccountClient {
    private final RestClient http;

    public CardAccountClient(RestClient.Builder builder, @Value("${mt.card-api-base-url:http://localhost:3000}") String baseUrl) {
        this.http = builder.baseUrl(trim(baseUrl)).build();
    }

    public Map<String, Object> findByPan(String pan, String entryMode) {
        if (pan == null || pan.isBlank()) {
            return null;
        }
        try {
            return http.get().uri("/internal/cards/by-pan?pan={pan}&entryMode={entryMode}", pan, entryMode == null ? "" : entryMode)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return null;
            }
            throw ex;
        }
    }

    private static String trim(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
