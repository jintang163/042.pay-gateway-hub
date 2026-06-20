package com.payhub.marketing.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class AdStatsOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String merchantNo;
    private LocalDate startDate;
    private LocalDate endDate;

    private Integer totalImpression;
    private Integer totalClick;
    private Integer totalValidClick;
    private Integer totalInvalidClick;
    private BigDecimal totalCost;
    private BigDecimal overallCtr;
    private BigDecimal overallAvgCpc;
    private Integer totalOrder;
    private BigDecimal totalOrderAmount;

    private List<AdDailyStatsVO> dailyStats;
    private List<AdItemStatsVO> adStats;
}
