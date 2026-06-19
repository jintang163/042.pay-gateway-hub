package com.payhub.settlement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WriteoffTypeEnum {

    SUPPLEMENT(1, "补单"),
    REFUND(2, "退款"),
    ADJUST(3, "调账"),
    IGNORE(4, "忽略");

    private final Integer code;
    private final String desc;

    public static WriteoffTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (WriteoffTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
