package com.payhub.marketing.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class ActivitySaveRequest {

    private Long id;

    @NotBlank(message = "商户号不能为空")
    private String merchantNo;

    @NotBlank(message = "活动名称不能为空")
    @Size(max = 100, message = "名称最多100字")
    private String activityName;

    @NotNull(message = "活动类型不能为空")
    private Integer activityType;

    private BigDecimal thresholdAmount;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    private BigDecimal maxDiscount;

    private String startTime;

    private String endTime;

    private String remark;
}
