package com.payhub.risk.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RuleTypeEnum {

    AMOUNT("AMOUNT", "金额规则"),
    FREQUENCY("FREQUENCY", "频率规则"),
    IP_BLACKLIST("IP_BLACKLIST", "IP黑名单规则"),
    DEVICE("DEVICE", "设备规则"),
    BEHAVIOR("BEHAVIOR", "行为规则"),
    WHITELIST("WHITELIST", "白名单规则");

    private final String code;

    private final String desc;

    public static RuleTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (RuleTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
