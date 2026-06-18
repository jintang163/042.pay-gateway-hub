package com.payhub.settlement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReconcileDiffTypeEnum {

    LONG_FUND(1, "长款", "渠道有记录，本地无记录"),
    SHORT_FUND(2, "短款", "本地有记录，渠道无记录"),
    AMOUNT_MISMATCH(3, "金额不一致", "本地与渠道金额不一致"),
    STATUS_MISMATCH(4, "状态不一致", "本地与渠道状态不一致");

    private final Integer code;
    private final String desc;
    private final String detail;

    public static ReconcileDiffTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReconcileDiffTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
