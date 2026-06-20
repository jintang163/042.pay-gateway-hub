package com.payhub.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("merchant_ad")
public class MerchantAd implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String adCode;

    private String merchantNo;

    private String adTitle;

    private String adDescription;

    private String adImageUrl;

    private String targetUrl;

    private String position;

    private BigDecimal cpcPrice;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private BigDecimal dailyBudget;

    private Integer clickCount;

    private Integer impressionCount;

    private BigDecimal totalCost;

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
