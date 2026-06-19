package com.payhub.marketing.enums;

import lombok.Getter;

@Getter
public enum CouponTypeEnum {

    FIXED_DISCOUNT(1, "固定金额抵扣"),
    PERCENT_DISCOUNT(2, "折扣率抵扣");

    private final Integer code;
    private final String desc;

    CouponTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CouponTypeEnum getByCode(Integer code) {
        for (CouponTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
