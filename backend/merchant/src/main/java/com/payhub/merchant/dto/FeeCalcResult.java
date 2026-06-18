package com.payhub.merchant.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FeeCalcResult {

    private BigDecimal amount;

    private BigDecimal feeAmount;

    private BigDecimal feeRate;

    private BigDecimal minFee;

    private BigDecimal maxFee;

    private String ruleNo;

    private String industryCode;

    private String industryName;

    private String payChannel;

    private String calcDetail;
}
