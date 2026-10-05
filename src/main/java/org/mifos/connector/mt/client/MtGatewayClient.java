package org.mifos.connector.mt.client;

import org.mifos.connector.mt.config.MtGatewayProperties;
import org.mifos.connector.mt.model.MtAuthorizationRequest;
import org.mifos.connector.mt.model.MtCaptureRequest;
import org.mifos.connector.mt.model.MtPaymentStatus;
import org.mifos.connector.mt.model.MtRefundRequest;
import org.mifos.connector.mt.model.MtVoidRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** HTTP client for Monétique Tunisie card payment gateway. */
@Component
public class MtGatewayClient {
    private static final Logger log = LoggerFactory.getLogger(MtGatewayClient.class);
    private final RestClient restClient;

    public MtGatewayClient(RestClient.Builder builder, MtGatewayProperties props) {
        RestClient.Builder b = builder.baseUrl(props.getGateway().getBaseUrl());
        if (props.getGateway().getApiKey() != null && !props.getGateway().getApiKey().isBlank()) {
            b = b.defaultHeader("X-API-Key", props.getGateway().getApiKey());
        }
        this.restClient = b.build();
    }

    public MtPaymentStatus authorize(MtAuthorizationRequest request) {
        log.info("MT authorize orderId={}", request.getOrderId());
        return restClient.post().uri("/mt/v1/authorizations").body(request).retrieve().body(MtPaymentStatus.class);
    }

    public MtPaymentStatus capture(MtCaptureRequest request) {
        return restClient.post().uri("/mt/v1/captures").body(request).retrieve().body(MtPaymentStatus.class);
    }

    public MtPaymentStatus refund(MtRefundRequest request) {
        return restClient.post().uri("/mt/v1/refunds").body(request).retrieve().body(MtPaymentStatus.class);
    }

    public MtPaymentStatus voidTransaction(MtVoidRequest request) {
        log.info("MT void originalTransactionId={}", request.getOriginalTransactionId());
        return restClient.post().uri("/mt/v1/voids").body(request).retrieve().body(MtPaymentStatus.class);
    }

    public MtPaymentStatus getStatus(String id) {
        return restClient.get().uri("/mt/v1/transactions/{id}", id).retrieve().body(MtPaymentStatus.class);
    }
}
