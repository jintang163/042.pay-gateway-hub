package com.payhub.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fee_promotion")
public class FeePromotion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String promotionNo;

    private String promotionName;

    private String promotionDesc;

    private Integer promotionType;

    private Integer targetType;

    private String targetValue;

    private Integer feeType;

    private BigDecimal discountFeeRate;

    private BigDecimal fixedFeeAmount;

    private BigDecimal maxDiscountAmount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer durationDays;

    private Integer totalQuota;

    private Integer usedQuota;

    private Integer perMerchantQuota;

    private Integer status;

    private String operatorId;

    private String operatorName;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
