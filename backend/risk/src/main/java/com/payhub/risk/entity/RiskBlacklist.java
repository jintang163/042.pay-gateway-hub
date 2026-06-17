package com.payhub.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("risk_blacklist")
public class RiskBlacklist implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String listType;

    private String listValue;

    private String listSource;

    private Integer riskLevel;

    private String reason;

    private String operatorId;

    private String operatorName;

    private Integer status;

    private LocalDateTime expireTime;

    private Integer hitCount;

    private LocalDateTime lastHitTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
