package com.payhub.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("risk_rule")
public class RiskRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
