package com.payhub.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FeePromotionFeeTypeEnum {

    RATE_DISCOUNT(1, "费率折扣"),
    FIXED_FEE(2, "固定手续费"),
    ZERO_FEE(3, "0手续费");

    private final Integer code;
    private final String desc;

    public static FeePromotionFeeTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FeePromotionFeeTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
