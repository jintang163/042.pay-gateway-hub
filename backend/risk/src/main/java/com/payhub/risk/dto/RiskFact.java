package com.payhub.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskFact implements Serializable {

    private static final long serialVersionUID = 1L;

    private String merchantNo;

    private String orderNo;

    private String clientIp;

    private String userIdentity;

    private String deviceId;

    private BigDecimal payAmount;

    private String payChannel;

    private String payType;

    private LocalDateTime requestTime;

    private Integer frequencyCount;

    private BigDecimal dailyAmount;

    private Integer deviceRiskScore;

    private Boolean whitelisted;

    private Boolean blacklisted;

    private Boolean ipBlacklisted;

    private Boolean behaviorAbnormal;

    private Boolean blocked;

    @Builder.Default
    private List<String> matchedRules = new ArrayList<>();

    private Integer riskLevel;

    private String actionType;

    @Builder.Default
    private StringBuilder riskDesc = new StringBuilder();

    public void addHitRule(String ruleCode, String ruleDesc, Integer level) {
        if (this.matchedRules == null) {
            this.matchedRules = new ArrayList<>();
        }
        this.matchedRules.add(ruleCode);
        if (this.riskDesc == null) {
            this.riskDesc = new StringBuilder();
        }
        this.riskDesc.append(ruleDesc).append("; ");
        if (this.riskLevel == null || level > this.riskLevel) {
            this.riskLevel = level;
        }
    }
}
