package com.payhub.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskCheckResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean pass;

    private Integer riskLevel;

    private List<String> riskRules;

    private String riskDesc;

    private String suggestion;

    private Boolean auditRequired;

    private Long auditId;

    private Boolean smsRequired;

    private String smsMobile;

    private Boolean whitelisted;
}
