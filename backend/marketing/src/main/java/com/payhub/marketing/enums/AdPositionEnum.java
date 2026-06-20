package com.payhub.marketing.enums;

import lombok.Getter;

@Getter
public enum AdPositionEnum {

    PAY_SUCCESS("PAY_SUCCESS", "支付成功页");

    private final String code;
    private final String desc;

    AdPositionEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AdPositionEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (AdPositionEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
