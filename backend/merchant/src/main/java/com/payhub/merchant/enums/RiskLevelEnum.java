package com.payhub.merchant.enums;

import lombok.Getter;

@Getter
public enum RiskLevelEnum {

    LOW("LOW", "低风险", 0, 30),
    MEDIUM("MEDIUM", "中风险", 31, 60),
    HIGH("HIGH", "高风险", 61, 100);

    private final String code;
    private final String name;
    private final Integer minScore;
    private final Integer maxScore;

    RiskLevelEnum(String code, String name, Integer minScore, Integer maxScore) {
        this.code = code;
        this.name = name;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public static RiskLevelEnum getByScore(Integer score) {
        if (score == null) {
            return LOW;
        }
        for (RiskLevelEnum e : values()) {
            if (score >= e.getMinScore() && score <= e.getMaxScore()) {
                return e;
            }
        }
        return HIGH;
    }

    public static RiskLevelEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (RiskLevelEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
