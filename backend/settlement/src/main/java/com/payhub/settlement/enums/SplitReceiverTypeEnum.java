package com.payhub.settlement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SplitReceiverTypeEnum {

    PERSONAL(1, "个人"),
    ENTERPRISE(2, "企业");

    private final Integer code;
    private final String desc;

    public static SplitReceiverTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (SplitReceiverTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
