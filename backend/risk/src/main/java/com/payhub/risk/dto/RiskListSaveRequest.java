package com.payhub.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskListSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String merchantNo;

    private String listType;

    private String listValue;

    private String listSource;

    private Integer riskLevel;

    private String reason;

    private LocalDateTime expireTime;

    private String bypassRules;
}
