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
public class OrderQueryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private String merchantOrderNo;

    private BigDecimal payAmount;

    private BigDecimal actualAmount;

    private BigDecimal feeAmount;

    private Integer payStatus;

    private LocalDateTime payTime;

    private String channelTradeNo;
}
