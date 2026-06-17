package com.payhub.channel.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class UnifiedOrderRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String merchantNo;

    private String orderNo;

    private BigDecimal amount;

    private String subject;

    private String detail;

    private String userIdentity;

    private String clientIp;

    private String payType;

    private String channelMerchantId;

    private String channelAppId;

    private String channelSecretKey;

    private String notifyUrl;

    private Map<String, String> extraParams;
}
