package com.payhub.marketing.service;

import com.payhub.marketing.dto.CouponDiscountCalcResult;

import java.math.BigDecimal;

public interface MarketingDiscountService {

    CouponDiscountCalcResult calcCouponDiscount(String couponCode, String merchantNo, BigDecimal orderAmount);

    BigDecimal calcActivityDiscount(String activityCode, String merchantNo, BigDecimal orderAmount);

    void recordCouponUse(String orderNo, String merchantNo, String couponCode,
                         BigDecimal orderAmount, BigDecimal discountAmount, String userId);

    void incrementPayLinkUsed(String linkCode);
}
