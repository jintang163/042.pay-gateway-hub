package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AgentProfitRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String ruleNo;

    private String ruleName;

    private String merchantNo;

    private String merchantName;

    private Integer agentLevel;

    private BigDecimal commissionRate;

    private BigDecimal minCommission;

    private Integer settleType;

    private String settleTypeDesc;

    private Integer status;

    private String statusDesc;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
