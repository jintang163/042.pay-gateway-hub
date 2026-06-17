package com.payhub.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String apiPath;

    private Long callCount;

    private Long successCount;

    private Long failCount;

    private Double successRate;

    private Double avgResponseTime;

    private Long minResponseTime;

    private Long maxResponseTime;
}
