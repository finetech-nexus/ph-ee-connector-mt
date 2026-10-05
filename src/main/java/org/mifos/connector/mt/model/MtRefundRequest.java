package org.mifos.connector.mt.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class MtRefundRequest {
    @NotBlank
    private String originalTransactionId;
    @NotNull
    private BigDecimal amount;
    private String currency;
    private String reason;
    private String callbackUrl;

    public String getOriginalTransactionId() { return originalTransactionId; }
    public void setOriginalTransactionId(String originalTransactionId) { this.originalTransactionId = originalTransactionId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
}
