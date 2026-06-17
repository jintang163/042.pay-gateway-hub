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
public class RefundResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String refundNo;

    private Integer refundStatus;

    private String channelRefundNo;

    private BigDecimal refundAmount;

    private LocalDateTime refundTime;
}
