package com.payhub.merchant.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class FeePromotionCalcResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal amount;

    private String promotionNo;

    private String promotionName;

    private Integer feeType;

    private String feeTypeDesc;

    private BigDecimal originalFeeRate;

    private BigDecimal discountFeeRate;

    private BigDecimal originalFee;

    private BigDecimal discountFee;

    private BigDecimal actualFee;

    private BigDecimal actualAmount;

    private BigDecimal savedAmount;

    private Boolean hasPromotion;

    private Boolean promotionApplied;

    private BigDecimal promoSaving;

    private String calcDetail;
}
