package com.payhub.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuditStatusEnum {

    PENDING(0, "待审核"),
    APPROVED(1, "审核通过"),
    REJECTED(2, "审核拒绝");

    private final Integer code;

    private final String desc;

    public static AuditStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AuditStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
