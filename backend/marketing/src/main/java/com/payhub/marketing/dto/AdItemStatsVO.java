package com.payhub.marketing.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AdItemStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String adCode;
    private String adTitle;
    private String position;
    private String positionDesc;
    private BigDecimal cpcPrice;
    private Integer impressionCount;
    private Integer clickCount;
    private Integer validClickCount;
    private Integer invalidClickCount;
    private BigDecimal totalCost;
    private BigDecimal ctr;
    private BigDecimal avgCpc;
    private Integer orderCount;
    private BigDecimal orderAmount;
    private Integer status;
    private String statusDesc;
}
