package org.mifos.connector.mt.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Card authorization request (Monétique Tunisie / ISO 8583-inspired REST). */
public class MtAuthorizationRequest {
    @NotBlank
    private String orderId;
    @NotNull
    private BigDecimal amount;
    @NotBlank
    private String currency;
    @NotBlank
    private String panToken;
    private String merchantId;
    private String terminalId;
    private String cardholderName;
    private String callbackUrl;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getPanToken() { return panToken; }
    public void setPanToken(String panToken) { this.panToken = panToken; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getTerminalId() { return terminalId; }
    public void setTerminalId(String terminalId) { this.terminalId = terminalId; }
    public String getCardholderName() { return cardholderName; }
    public void setCardholderName(String cardholderName) { this.cardholderName = cardholderName; }
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
}
