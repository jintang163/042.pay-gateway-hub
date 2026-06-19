package com.payhub.marketing.enums;

import lombok.Getter;

@Getter
public enum CouponStatusEnum {

    NOT_STARTED(0, "未开始"),
    ACTIVE(1, "发放中"),
    PAUSED(2, "已暂停"),
    EXHAUSTED(3, "已发完"),
    EXPIRED(4, "已过期");

    private final Integer code;
    private final String desc;

    CouponStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CouponStatusEnum getByCode(Integer code) {
        for (CouponStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
