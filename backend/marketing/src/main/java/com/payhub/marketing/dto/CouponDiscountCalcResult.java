package com.payhub.marketing.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponDiscountCalcResult {

    private String couponCode;

    private String couponName;

    private Integer couponType;

    private String couponTypeDesc;

    private BigDecimal orderAmount;

    private BigDecimal discountAmount;

    private BigDecimal actualAmount;

    private String calcDetail;
}
