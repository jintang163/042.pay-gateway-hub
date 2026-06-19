package com.payhub.marketing.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class CouponSaveRequest {

    private Long id;

    @NotBlank(message = "商户号不能为空")
    private String merchantNo;

    @NotBlank(message = "优惠券名称不能为空")
    @Size(max = 100, message = "名称最多100字")
    private String couponName;

    @NotNull(message = "优惠券类型不能为空")
    private Integer couponType;

    @NotNull(message = "优惠值不能为空")
    private BigDecimal discountValue;

    private BigDecimal minOrderAmount;

    private BigDecimal maxDiscount;

    @NotNull(message = "发放总量不能为空")
    @Min(value = 1, message = "发放总量至少1")
    private Integer totalQuantity;

    private String startTime;

    private String endTime;

    private String remark;
}
