package com.payhub.channel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "pay.channel")
public class ChannelProperties {

    private AlipayProperties alipay = new AlipayProperties();

    private WechatProperties wechat = new WechatProperties();

    private UnionPayProperties unionPay = new UnionPayProperties();

    @Data
    public static class AlipayProperties {
        private String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";
        private String appId;
        private String merchantPrivateKey;
        private String alipayPublicKey;
        private String notifyUrl;
        private String charset = "utf-8";
        private String signType = "RSA2";
        private String format = "json";
        private int sandboxMode = 1;
    }

    @Data
    public static class WechatProperties {
        private String gatewayUrl = "https://api.mch.weixin.qq.com";
        private String appId;
        private String mchId;
        private String apiKey;
        private String apiV3Key;
        private String mchSerialNo;
        private String privateKeyPath;
        private String notifyUrl;
        private int sandboxMode = 1;
    }

    @Data
    public static class UnionPayProperties {
        private String gatewayUrl = "https://gateway.95516.com";
        private String merId;
        private String merName;
        private String appId;
        private String privateKey;
        private String publicKey;
        private String middleCert;
        private String rootCert;
        private String notifyUrl;
        private String version = "5.1.0";
        private String encoding = "UTF-8";
        private String signMethod = "01";
        private String txnType = "01";
        private String txnSubType = "01";
        private String bizType = "000201";
        private String channelType = "08";
        private String accessType = "0";
        private String currencyCode = "156";
        private int sandboxMode = 1;
    }
}
