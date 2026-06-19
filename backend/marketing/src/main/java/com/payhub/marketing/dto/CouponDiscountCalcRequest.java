package com.payhub.marketing.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class CouponDiscountCalcRequest {

    @NotBlank(message = "优惠券编码不能为空")
    private String couponCode;

    @NotNull(message = "订单金额不能为空")
    private BigDecimal orderAmount;

    private String merchantNo;
}
