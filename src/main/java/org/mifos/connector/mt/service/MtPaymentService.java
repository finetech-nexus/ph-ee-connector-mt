package org.mifos.connector.mt.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.mifos.connector.mt.client.MtGatewayClient;
import org.mifos.connector.mt.config.MtGatewayProperties;
import org.mifos.connector.mt.model.MtAuthorizationRequest;
import org.mifos.connector.mt.model.MtCaptureRequest;
import org.mifos.connector.mt.model.MtPaymentStatus;
import org.mifos.connector.mt.model.MtRefundRequest;
import org.mifos.connector.mt.model.MtVoidRequest;
import org.springframework.stereotype.Service;

@Service
public class MtPaymentService {
    private final MtGatewayClient client;
    private final MtGatewayProperties props;
    private final Map<String, MtPaymentStatus> cache = new ConcurrentHashMap<>();

    public MtPaymentService(MtGatewayClient client, MtGatewayProperties props) {
        this.client = client;
        this.props = props;
    }

    public MtPaymentStatus authorize(MtAuthorizationRequest request) {
        if (request.getMerchantId() == null || request.getMerchantId().isBlank()) {
            request.setMerchantId(props.getGateway().getMerchantId());
        }
        if (request.getCallbackUrl() == null || request.getCallbackUrl().isBlank()) {
            request.setCallbackUrl(props.getCallbackBaseUrl() + "/api/v1/mt/callbacks");
        }
        return put(client.authorize(request));
    }

    public MtPaymentStatus capture(MtCaptureRequest request) {
        if (request.getCallbackUrl() == null || request.getCallbackUrl().isBlank()) {
            request.setCallbackUrl(props.getCallbackBaseUrl() + "/api/v1/mt/callbacks");
        }
        return put(client.capture(request));
    }

    public MtPaymentStatus refund(MtRefundRequest request) {
        if (request.getCallbackUrl() == null || request.getCallbackUrl().isBlank()) {
            request.setCallbackUrl(props.getCallbackBaseUrl() + "/api/v1/mt/callbacks");
        }
        return put(client.refund(request));
    }

    public MtPaymentStatus voidTransaction(MtVoidRequest request) {
        if (request.getCallbackUrl() == null || request.getCallbackUrl().isBlank()) {
            request.setCallbackUrl(props.getCallbackBaseUrl() + "/api/v1/mt/callbacks");
        }
        return put(client.voidTransaction(request));
    }

    public MtPaymentStatus status(String id) { return put(client.getStatus(id)); }

    public MtPaymentStatus acceptCallback(MtPaymentStatus status) { return put(status); }

    private MtPaymentStatus put(MtPaymentStatus status) {
        if (status == null) return null;
        if (status.getTransactionId() != null) cache.put(status.getTransactionId(), status);
        if (status.getAuthorizationId() != null) cache.put(status.getAuthorizationId(), status);
        if (status.getOrderId() != null) cache.put(status.getOrderId(), status);
        return status;
    }
}
