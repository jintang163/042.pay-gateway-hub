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
public class RiskRuleSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String ruleCode;

    private String ruleName;

    private String ruleType;

    private Integer riskLevel;

    private String ruleCondition;

    private String ruleContent;

    private String actionType;

    private String smsTemplateId;

    private Integer priority;

    private Integer status;

    private LocalDateTime effectStartTime;

    private LocalDateTime effectEndTime;

    private String remark;
}
