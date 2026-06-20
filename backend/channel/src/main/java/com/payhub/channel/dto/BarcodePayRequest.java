package com.payhub.channel.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class BarcodePayRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String merchantNo;

    private String orderNo;

    private BigDecimal amount;

    private String subject;

    private String detail;

    private String clientIp;

    private String authCode;

    private String scene;

    private String channelMerchantId;

    private String channelAppId;

    private String channelSecretKey;

    private String notifyUrl;
}
