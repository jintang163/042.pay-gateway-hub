package com.payhub.settlement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReconcileHandleStatusEnum {

    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    PROCESSED(2, "已处理"),
    IGNORED(3, "已忽略");

    private final Integer code;
    private final String desc;

    public static ReconcileHandleStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReconcileHandleStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
