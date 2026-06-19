package com.payhub.marketing.enums;

import lombok.Getter;

@Getter
public enum PayLinkStatusEnum {

    ACTIVE(1, "生效中"),
    EXPIRED(2, "已过期"),
    DISABLED(3, "已禁用"),
    EXHAUSTED(4, "已用尽");

    private final Integer code;
    private final String desc;

    PayLinkStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PayLinkStatusEnum getByCode(Integer code) {
        for (PayLinkStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
