package com.payhub.marketing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ActivityVO {

    private Long id;

    private String activityCode;

    private String merchantNo;

    private String activityName;

    private Integer activityType;

    private String activityTypeDesc;

    private BigDecimal thresholdAmount;

    private BigDecimal discountAmount;

    private BigDecimal discountRate;

    private BigDecimal maxDiscount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private String statusDesc;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
