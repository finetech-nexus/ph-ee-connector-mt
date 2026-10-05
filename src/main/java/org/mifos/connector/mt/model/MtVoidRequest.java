package org.mifos.connector.mt.model;

import jakarta.validation.constraints.NotBlank;

/** Void / reversal of authorization or capture (ISO 8583 message type 0400-style). */
public class MtVoidRequest {
    @NotBlank
    private String originalTransactionId;
    private String authorizationId;
    private String reason;
    private String callbackUrl;

    public String getOriginalTransactionId() { return originalTransactionId; }
    public void setOriginalTransactionId(String originalTransactionId) { this.originalTransactionId = originalTransactionId; }
    public String getAuthorizationId() { return authorizationId; }
    public void setAuthorizationId(String authorizationId) { this.authorizationId = authorizationId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
}
