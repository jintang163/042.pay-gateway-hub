package com.payhub.marketing.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AdDailyStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate statsDate;
    private Integer impressionCount;
    private Integer clickCount;
    private Integer validClickCount;
    private Integer invalidClickCount;
    private BigDecimal totalCost;
    private BigDecimal ctr;
    private BigDecimal avgCpc;
    private Integer orderCount;
    private BigDecimal orderAmount;
}
