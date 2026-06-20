package com.payhub.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FeePromotionTypeEnum {

    NEW_MERCHANT(1, "新商户优惠"),
    HOLIDAY(2, "节日特惠"),
    ANNIVERSARY(3, "周年庆"),
    VIP(4, "VIP专属"),
    CUSTOM(5, "自定义活动");

    private final Integer code;
    private final String desc;

    public static FeePromotionTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FeePromotionTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
