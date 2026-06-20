package com.payhub.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ad_stats_daily")
public class AdStatsDaily implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate statsDate;

    private String adCode;

    private String merchantNo;

    private String position;

    private Integer impressionCount;

    private Integer clickCount;

    private Integer validClickCount;

    private Integer invalidClickCount;

    private BigDecimal totalCost;

    private BigDecimal ctr;

    private BigDecimal avgCpc;

    private Integer orderCount;

    private BigDecimal orderAmount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
