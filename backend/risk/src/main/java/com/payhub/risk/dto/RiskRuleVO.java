package com.payhub.risk.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class RiskRuleVO implements Serializable {

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

    private String statusDesc;

    private LocalDateTime effectStartTime;

    private LocalDateTime effectEndTime;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
