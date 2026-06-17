package com.payhub.risk.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class RiskListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String listType;

    private String listValue;

    private String listSource;

    private Integer riskLevel;

    private String reason;

    private String operatorId;

    private String operatorName;

    private Integer status;

    private String statusDesc;

    private LocalDateTime expireTime;

    private Integer hitCount;

    private LocalDateTime lastHitTime;

    private String bypassRules;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
