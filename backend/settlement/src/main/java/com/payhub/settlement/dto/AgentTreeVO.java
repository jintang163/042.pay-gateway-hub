package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class AgentTreeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String merchantNo;

    private String merchantName;

    private Integer agentLevel;

    private BigDecimal commissionRate;

    private Integer status;

    private List<AgentTreeVO> children;
}
