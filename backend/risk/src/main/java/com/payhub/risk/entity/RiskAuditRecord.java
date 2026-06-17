package com.payhub.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("risk_audit_record")
public class RiskAuditRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String auditNo;

    private Long riskLogId;

    private String merchantNo;

    private String orderNo;

    private String auditType;

    private Integer auditLevel;

    private Integer auditStatus;

    private Integer riskLevelBefore;

    private Integer riskLevelAfter;

    private String auditResult;

    private String auditRemark;

    private String auditUserId;

    private String auditUserName;

    private LocalDateTime auditTime;

    private Integer smsVerified;

    private String smsMobile;

    private LocalDateTime smsVerifyTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
