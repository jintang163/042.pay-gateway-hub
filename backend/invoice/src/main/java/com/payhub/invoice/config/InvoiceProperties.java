package com.payhub.invoice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "payhub.invoice")
public class InvoiceProperties {

    private NuoNuoConfig nuonuo = new NuoNuoConfig();

    private BaiWangConfig baiwang = new BaiWangConfig();

    private String defaultChannel = "NUONUO";

    private String callbackBaseUrl = "http://localhost:8080";

    private int connectTimeout = 10000;

    private int readTimeout = 30000;

    @Data
    public static class NuoNuoConfig {
        private String appKey;
        private String appSecret;
        private String accessToken;
        private String taxNum;
        private String baseUrl = "https://sdk.nuonuo.com";
        private boolean enabled = false;
    }

    @Data
    public static class BaiWangConfig {
        private String appId;
        private String appSecret;
        private String taxNum;
        private String baseUrl = "https://api.baiwang.com";
        private boolean enabled = false;
    }
}
