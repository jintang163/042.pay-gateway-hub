package com.payhub.common.crypto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "payhub.crypto")
public class CryptoAutoConfiguration {

    private String sm4Key;

    private String sm4Iv;

    private String sm2PublicKey;

    private String sm2PrivateKey;

    private String rsaPublicKey;

    private String rsaPrivateKey;

    private String md5Salt;
}
