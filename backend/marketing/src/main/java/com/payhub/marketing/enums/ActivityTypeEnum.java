package com.payhub.marketing.enums;

import lombok.Getter;

@Getter
public enum ActivityTypeEnum {

    FULL_REDUCTION(1, "满减活动"),
    DISCOUNT(2, "折扣活动");

    private final Integer code;
    private final String desc;

    ActivityTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ActivityTypeEnum getByCode(Integer code) {
        for (ActivityTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
