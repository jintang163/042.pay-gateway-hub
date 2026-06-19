package com.payhub.marketing.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.marketing.dto.CouponDiscountCalcRequest;
import com.payhub.marketing.dto.CouponDiscountCalcResult;
import com.payhub.marketing.dto.CouponSaveRequest;
import com.payhub.marketing.dto.CouponVO;

import java.util.Map;

public interface CouponService {

    IPage<CouponVO> listPage(Long current, Long size, String merchantNo, Map<String, Object> params);

    CouponVO getByCouponCode(String couponCode);

    void saveCoupon(CouponSaveRequest request);

    void toggleStatus(Long id);

    void deleteCoupon(Long id);

    CouponDiscountCalcResult calculateDiscount(CouponDiscountCalcRequest request);
}
