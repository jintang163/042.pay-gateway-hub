package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SplitDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String splitDetailNo;

    private Long settlementId;

    private String settlementNo;

    private String orderNo;

    private String merchantNo;

    private String ruleNo;

    private String receiverAccount;

    private String receiverName;

    private String splitType;

    private BigDecimal splitValue;

    private BigDecimal splitAmount;

    private Integer status;

    private String statusDesc;

    private LocalDateTime settleTime;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
