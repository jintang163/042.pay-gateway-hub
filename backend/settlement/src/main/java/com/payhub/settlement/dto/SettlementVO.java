package com.payhub.settlement.dto;

import com.payhub.settlement.entity.PaySplitDetail;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SettlementVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String settlementNo;

    private String merchantNo;

    private String payChannel;

    private LocalDate settleDate;

    private BigDecimal totalAmount;

    private BigDecimal feeAmount;

    private BigDecimal actualSettleAmount;

    private Integer orderCount;

    private Integer settleStatus;

    private String settleStatusDesc;

    private String bankName;

    private String bankAccount;

    private String accountName;

    private String failReason;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;

    private LocalDateTime settleTime;

    private List<PaySplitDetail> splitDetails;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
