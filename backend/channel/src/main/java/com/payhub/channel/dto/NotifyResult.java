package com.payhub.channel.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class NotifyResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private String channelTradeNo;

    private String payStatus;

    private BigDecimal payAmount;

    private LocalDateTime payTime;

    private String merchantNo;

    private String refundNo;

    private String channelRefundNo;

    private String refundStatus;
}
