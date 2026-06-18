package com.payhub.merchant.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FeeRuleVO {

    private Long id;

    private String ruleNo;

    private String industryCode;

    private String industryName;

    private String payChannel;

    private String payChannelDesc;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private BigDecimal feeRate;

    private BigDecimal minFee;

    private BigDecimal maxFee;

    private Integer priority;

    private Integer status;

    private String statusDesc;

    private String operatorName;

    private String remark;

    private String createdAt;
}
