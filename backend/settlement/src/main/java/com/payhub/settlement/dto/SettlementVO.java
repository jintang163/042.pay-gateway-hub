package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SettlementVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String settlementNo;

    private String merchantNo;

    private LocalDate settleDate;

    private BigDecimal totalAmount;

    private BigDecimal feeAmount;

    private BigDecimal actualSettleAmount;

    private Integer orderCount;

    private Integer settleStatus;

    private String settleStatusDesc;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
