package com.payhub.merchant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "business-api")
public class BusinessApiProperties {

    private Boolean sandbox = true;

    private String url;

    private String appKey;

    private String appSecret;

    private Integer timeoutMs = 5000;

    private Integer retryTimes = 2;
}
