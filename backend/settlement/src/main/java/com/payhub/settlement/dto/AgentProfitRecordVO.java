package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AgentProfitRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String profitNo;

    private String orderNo;

    private String merchantNo;

    private String merchantName;

    private String agentMerchantNo;

    private String agentMerchantName;

    private Integer agentLevel;

    private BigDecimal orderAmount;

    private BigDecimal feeAmount;

    private BigDecimal profitAmount;

    private BigDecimal commissionRate;

    private String settleDate;

    private Integer profitStatus;

    private String profitStatusDesc;

    private String settlementId;

    private String settlementNo;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
