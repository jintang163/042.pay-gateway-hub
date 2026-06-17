package com.payhub.channel.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class RefundRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private String refundNo;

    private BigDecimal refundAmount;

    private String refundReason;

    private String channelMerchantId;

    private String channelSecretKey;

    private String channelTradeNo;
}
