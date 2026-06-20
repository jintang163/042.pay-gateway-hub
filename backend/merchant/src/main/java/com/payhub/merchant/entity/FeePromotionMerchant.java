package com.payhub.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fee_promotion_merchant")
public class FeePromotionMerchant implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String promotionNo;

    private String merchantNo;

    private String merchantName;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer totalQuota;

    private Integer usedQuota;

    private BigDecimal totalDiscountAmount;

    private Integer status;

    private String bindSource;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
