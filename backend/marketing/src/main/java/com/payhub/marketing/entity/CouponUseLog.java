package com.payhub.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("coupon_use_log")
public class CouponUseLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String couponCode;

    private String merchantNo;

    private String orderNo;

    private String userId;

    private BigDecimal orderAmount;

    private BigDecimal discountAmount;

    private Integer useType;

    private LocalDateTime usedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
