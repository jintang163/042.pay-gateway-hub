package com.payhub.marketing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponVO {

    private Long id;

    private String couponCode;

    private String merchantNo;

    private String couponName;

    private Integer couponType;

    private String couponTypeDesc;

    private BigDecimal discountValue;

    private BigDecimal minOrderAmount;

    private BigDecimal maxDiscount;

    private Integer totalQuantity;

    private Integer issuedCount;

    private Integer usedCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private String statusDesc;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
