package com.payhub.risk.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RiskLevelEnum {

    LOW(1, "低风险"),
    MEDIUM(2, "中风险"),
    HIGH(3, "高风险");

    private final Integer code;

    private final String desc;

    public static RiskLevelEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (RiskLevelEnum level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        return null;
    }
}
