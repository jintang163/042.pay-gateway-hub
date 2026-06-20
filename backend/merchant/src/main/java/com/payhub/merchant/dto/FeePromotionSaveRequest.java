package com.payhub.merchant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FeePromotionSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "活动名称不能为空")
    private String promotionName;

    private String promotionDesc;

    @NotNull(message = "活动类型不能为空")
    private Integer promotionType;

    @NotNull(message = "目标类型不能为空")
    private Integer targetType;

    private String targetValue;

    @NotNull(message = "优惠类型不能为空")
    private Integer feeType;

    private BigDecimal discountFeeRate;

    private BigDecimal fixedFeeAmount;

    private BigDecimal maxDiscountAmount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer durationDays;

    private Integer totalQuota;

    private Integer perMerchantQuota;

    private Integer status;

    private List<String> merchantNoList;

    private String remark;
}
