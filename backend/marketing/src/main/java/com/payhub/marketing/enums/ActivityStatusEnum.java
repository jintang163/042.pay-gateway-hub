package com.payhub.marketing.enums;

import lombok.Getter;

@Getter
public enum ActivityStatusEnum {

    NOT_STARTED(0, "未开始"),
    ACTIVE(1, "进行中"),
    PAUSED(2, "已暂停"),
    ENDED(3, "已结束");

    private final Integer code;
    private final String desc;

    ActivityStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ActivityStatusEnum getByCode(Integer code) {
        for (ActivityStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
