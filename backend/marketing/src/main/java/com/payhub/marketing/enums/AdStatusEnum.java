package com.payhub.marketing.enums;

import lombok.Getter;

@Getter
public enum AdStatusEnum {

    OFFLINE(0, "已下架"),
    ONLINE(1, "已上架");

    private final Integer code;
    private final String desc;

    AdStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AdStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AdStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
