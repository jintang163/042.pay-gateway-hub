package com.payhub.settlement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WriteoffSourceEnum {

    AUTO(1, "自动"),
    MANUAL(2, "手动");

    private final Integer code;
    private final String desc;

    public static WriteoffSourceEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (WriteoffSourceEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
