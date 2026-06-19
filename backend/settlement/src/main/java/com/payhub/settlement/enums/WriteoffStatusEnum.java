package com.payhub.settlement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WriteoffStatusEnum {

    PENDING(0, "待执行"),
    EXECUTING(1, "执行中"),
    SUCCESS(2, "成功"),
    FAIL(3, "失败");

    private final Integer code;
    private final String desc;

    public static WriteoffStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (WriteoffStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
