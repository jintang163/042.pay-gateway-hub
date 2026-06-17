package com.payhub.risk.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ListTypeEnum {

    IP("IP", "IP地址"),
    USER("USER", "用户"),
    MERCHANT("MERCHANT", "商户"),
    DEVICE("DEVICE", "设备");

    private final String code;

    private final String desc;

    public static ListTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (ListTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
