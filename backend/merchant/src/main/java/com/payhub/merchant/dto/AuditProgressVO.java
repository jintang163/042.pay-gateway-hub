package com.payhub.merchant.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AuditProgressVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String merchantNo;

    private String merchantName;

    private Integer auditStatus;

    private String auditStatusDesc;

    private Integer auditStep;

    private String auditStepName;

    private String auditStepDescription;

    private String riskLevel;

    private String riskLevelDesc;

    private Integer riskScore;

    private Integer businessVerifyPassed;

    private String businessVerifyResult;

    private LocalDateTime businessVerifyTime;

    private Integer autoAuditPassed;

    private String autoAuditRemark;

    private LocalDateTime autoAuditTime;

    private String manualAuditUser;

    private LocalDateTime manualAuditTime;

    private String auditRemark;

    private List<AuditStepItem> steps;

    @Data
    public static class AuditStepItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer step;
        private String name;
        private String description;
        private String status;
        private LocalDateTime time;
        private String remark;
    }
}
