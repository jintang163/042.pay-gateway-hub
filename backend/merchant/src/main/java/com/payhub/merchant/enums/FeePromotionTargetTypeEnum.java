package com.payhub.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FeePromotionTargetTypeEnum {

    ALL_MERCHANT(1, "全体商户"),
    INDUSTRY(2, "指定行业"),
    DESIGNATED(3, "指定商户"),
    NEW_REGISTER(4, "新注册商户");

    private final Integer code;
    private final String desc;

    public static FeePromotionTargetTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FeePromotionTargetTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
