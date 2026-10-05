package org.mifos.connector.mt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mt")
public class MtGatewayProperties {
    private Gateway gateway = new Gateway();
    private String callbackBaseUrl = "http://localhost:8087";

    public Gateway getGateway() { return gateway; }
    public void setGateway(Gateway gateway) { this.gateway = gateway; }
    public String getCallbackBaseUrl() { return callbackBaseUrl; }
    public void setCallbackBaseUrl(String callbackBaseUrl) { this.callbackBaseUrl = callbackBaseUrl; }

    public static class Gateway {
        private String baseUrl = "http://localhost:8090";
        private String apiKey = "";
        private String merchantId = "NEXUS-MERCHANT";
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getMerchantId() { return merchantId; }
        public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    }
}
