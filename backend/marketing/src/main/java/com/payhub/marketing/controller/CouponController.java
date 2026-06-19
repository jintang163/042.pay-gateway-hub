package com.payhub.marketing.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.marketing.dto.CouponDiscountCalcRequest;
import com.payhub.marketing.dto.CouponDiscountCalcResult;
import com.payhub.marketing.dto.CouponSaveRequest;
import com.payhub.marketing.dto.CouponVO;
import com.payhub.marketing.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/coupon")
public class CouponController {

    @Autowired
    private CouponService couponService;

    @GetMapping("/list")
    public Result<IPage<CouponVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String couponCode,
            @RequestParam(required = false) String couponName,
            @RequestParam(required = false) Integer couponType,
            @RequestParam(required = false) Integer status,
            HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        Map<String, Object> params = new HashMap<>();
        params.put("couponCode", couponCode);
        params.put("couponName", couponName);
        params.put("couponType", couponType);
        params.put("status", status);
        IPage<CouponVO> page = couponService.listPage(current, size, merchantNo, params);
        return Result.success(page);
    }

    @GetMapping("/{couponCode}")
    public Result<CouponVO> getByCouponCode(@PathVariable String couponCode) {
        return Result.success(couponService.getByCouponCode(couponCode));
    }

    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody CouponSaveRequest request,
                             HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        if (merchantNo != null) {
            request.setMerchantNo(merchantNo);
        }
        couponService.saveCoupon(request);
        return Result.success();
    }

    @PostMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable Long id) {
        couponService.toggleStatus(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return Result.success();
    }

    @PostMapping("/calculate-discount")
    public Result<CouponDiscountCalcResult> calculateDiscount(@Valid @RequestBody CouponDiscountCalcRequest request,
                                                               HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        if (merchantNo != null) {
            request.setMerchantNo(merchantNo);
        }
        return Result.success(couponService.calculateDiscount(request));
    }
}
