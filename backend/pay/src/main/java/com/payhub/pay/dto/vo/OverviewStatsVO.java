package com.payhub.pay.dto.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverviewStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal todayAmount;

    private Long todayOrderCount;

    private BigDecimal successRate;

    private Long merchantCount;
}
