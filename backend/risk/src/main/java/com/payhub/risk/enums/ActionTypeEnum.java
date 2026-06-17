package com.payhub.risk.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ActionTypeEnum {

    PASS("PASS", "放行"),
    BLOCK("BLOCK", "拦截"),
    SMS("SMS", "短信验证"),
    MANUAL("MANUAL", "人工审核");

    private final String code;

    private final String desc;

    public static ActionTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (ActionTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
