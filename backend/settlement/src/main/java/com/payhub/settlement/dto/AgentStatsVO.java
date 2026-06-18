package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AgentStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer totalAgentCount;

    private Integer activeAgentCount;

    private BigDecimal totalProfitAmount;

    private BigDecimal availableBalance;

    private BigDecimal frozenAmount;

    private Integer todayNewAgentCount;

    private Integer totalSubordinateCount;

    private Integer totalOrderCount;

    private BigDecimal totalOrderAmount;

    private Integer todayOrderCount;

    private BigDecimal todayOrderAmount;
}
