package org.mifos.connector.mt.model;

import java.time.Instant;
import java.util.Map;

public class MtPaymentStatus {
    private String transactionId;
    private String authorizationId;
    private String orderId;
    private String status; // AUTHORIZED, CAPTURED, SETTLED, FAILED, REFUNDED
    private String responseCode; // ISO8583-like 00, 05, ...
    private String authCode;
    private String reasonMessage;
    private Instant updatedAt;
    private Map<String, Object> details;

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getAuthorizationId() { return authorizationId; }
    public void setAuthorizationId(String authorizationId) { this.authorizationId = authorizationId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }
    public String getAuthCode() { return authCode; }
    public void setAuthCode(String authCode) { this.authCode = authCode; }
    public String getReasonMessage() { return reasonMessage; }
    public void setReasonMessage(String reasonMessage) { this.reasonMessage = reasonMessage; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}
