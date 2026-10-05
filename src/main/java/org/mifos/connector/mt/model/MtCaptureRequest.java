package org.mifos.connector.mt.model;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class MtCaptureRequest {
    @NotBlank
    private String authorizationId;
    private BigDecimal amount;
    private String callbackUrl;

    public String getAuthorizationId() { return authorizationId; }
    public void setAuthorizationId(String authorizationId) { this.authorizationId = authorizationId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
}
