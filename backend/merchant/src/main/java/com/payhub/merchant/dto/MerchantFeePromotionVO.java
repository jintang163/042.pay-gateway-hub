package com.payhub.merchant.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MerchantFeePromotionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String promotionNo;

    private String promotionName;

    private String promotionDesc;

    private Integer promotionType;

    private String promotionTypeDesc;

    private String promotionTypeName;

    private Integer feeType;

    private String feeTypeDesc;

    private BigDecimal discountFeeRate;

    private BigDecimal fixedFeeAmount;

    private BigDecimal maxDiscountAmount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer totalQuota;

    private Integer usedQuota;

    private BigDecimal totalDiscountAmount;

    private Integer status;

    private String statusDesc;

    private Long remainingSeconds;

    private String countdownText;

    private String remark;

    private LocalDateTime createdAt;
}
