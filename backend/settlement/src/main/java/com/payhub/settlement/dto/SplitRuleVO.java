package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SplitRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String ruleNo;

    private String merchantNo;

    private String ruleName;

    private String splitDetails;

    private Integer status;

    private String statusDesc;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
