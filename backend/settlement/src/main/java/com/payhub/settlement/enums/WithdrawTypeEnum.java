package com.payhub.settlement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public enum WithdrawTypeEnum {

    T1(1, "T+1到账", new BigDecimal("0"), "下一个工作日到账，免手续费"),
    INSTANT(2, "即时到账", new BigDecimal("0.1"), "实时到账，收取0.1%手续费");

    private final Integer code;
    private final String desc;
    private final BigDecimal feeRate;
    private final String remark;

    public static WithdrawTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (WithdrawTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
