package com.payhub.merchant.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FeePromotionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String promotionNo;

    private String promotionName;

    private String promotionDesc;

    private Integer promotionType;

    private String promotionTypeDesc;

    private Integer targetType;

    private String targetTypeDesc;

    private String targetValue;

    private Integer feeType;

    private String feeTypeDesc;

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

    private String statusDesc;

    private String operatorId;

    private String operatorName;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
