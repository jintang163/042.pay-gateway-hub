package com.payhub.settlement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AgentProfitStatusEnum {

    PENDING(0, "待结算"),
    SETTLED(1, "已结算"),
    FAILED(2, "结算失败");

    private final Integer code;
    private final String desc;

    public static AgentProfitStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AgentProfitStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
