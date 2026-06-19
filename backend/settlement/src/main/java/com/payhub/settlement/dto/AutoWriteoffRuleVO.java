package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AutoWriteoffRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String ruleName;

    private String merchantNo;

    private String payChannel;

    private String payChannelDesc;

    private Integer diffType;

    private String diffTypeDesc;

    private BigDecimal maxAmount;

    private Integer autoWriteoff;

    private String autoWriteoffDesc;

    private Integer handleType;

    private String handleTypeDesc;

    private Integer enabled;

    private String enabledDesc;

    private Integer priority;

    private String remark;

    private String operatorId;

    private String operatorName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
