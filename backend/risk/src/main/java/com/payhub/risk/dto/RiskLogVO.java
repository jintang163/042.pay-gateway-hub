package com.payhub.risk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String merchantNo;

    private String orderNo;

    private String riskType;

    private Integer riskLevel;

    private String riskRule;

    private String riskDesc;

    private String clientIp;

    private String userIdentity;

    private BigDecimal payAmount;

    private String payChannel;

    private Integer handleResult;

    private String handleDesc;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime triggerTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
