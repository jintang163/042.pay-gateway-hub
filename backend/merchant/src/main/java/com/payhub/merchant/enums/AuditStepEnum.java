package com.payhub.merchant.enums;

import lombok.Getter;

@Getter
public enum AuditStepEnum {

    DATA_SUBMITTED(1, "资料提交", "已提交入驻申请"),
    BUSINESS_VERIFYING(2, "工商核验中", "正在核验营业执照信息"),
    BUSINESS_VERIFIED(3, "工商核验完成", "营业执照核验完成"),
    RISK_EVALUATING(4, "风险评估中", "正在进行风险评估"),
    RISK_EVALUATED(5, "评估完成", "风险评估完成"),
    AUTO_AUDIT_DONE(6, "自动审核完成", "自动审核完成，等待人工复核或已通过"),
    MANUAL_AUDITING(7, "人工审核中", "高风险商户，转人工审核");

    private final Integer code;
    private final String name;
    private final String description;

    AuditStepEnum(Integer code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static AuditStepEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AuditStepEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
