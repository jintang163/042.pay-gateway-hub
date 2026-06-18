package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AgentRelationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String merchantNo;

    private String merchantName;

    private String parentMerchantNo;

    private String parentMerchantName;

    private Integer agentLevel;

    private String agentPath;

    private BigDecimal commissionRate;

    private Integer status;

    private String statusDesc;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
