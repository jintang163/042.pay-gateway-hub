package com.payhub.settlement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AgentSettleTypeEnum {

    SEPARATE(0, "单独结算"),
    STACKED(1, "叠加分账");

    private final Integer code;
    private final String desc;

    public static AgentSettleTypeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AgentSettleTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
