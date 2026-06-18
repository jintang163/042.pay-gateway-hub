package com.payhub.merchant.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FeeCalcResult {

    private Long amount;

    private Long feeAmount;

    private BigDecimal feeRate;

    private Long minFee;

    private Long maxFee;

    private String ruleNo;

    private String industryCode;

    private String industryName;

    private String payChannel;

    private String calcDetail;
}
