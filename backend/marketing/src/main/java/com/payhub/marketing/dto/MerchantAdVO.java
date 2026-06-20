package com.payhub.marketing.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MerchantAdVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String adCode;
    private String merchantNo;
    private String adTitle;
    private String adDescription;
    private String adImageUrl;
    private String targetUrl;
    private String position;
    private String positionDesc;
    private BigDecimal cpcPrice;
    private Integer sortOrder;
    private Integer status;
    private String statusDesc;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal dailyBudget;
    private Integer clickCount;
    private Integer impressionCount;
    private BigDecimal totalCost;
    private BigDecimal ctr;
    private String operatorId;
    private String operatorName;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
