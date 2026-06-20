package com.payhub.pay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregateCodeQueryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private String merchantNo;

    private String merchantOrderNo;

    private String payChannel;

    private String payType;

    private BigDecimal payAmount;

    private Integer payStatus;

    private String payStatusDesc;

    private String channelTradeNo;

    private LocalDateTime payTime;

    private LocalDateTime createTime;

    private LocalDateTime expireTime;
}
