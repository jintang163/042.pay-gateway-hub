package com.payhub.settlement.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AgentProfitRuleSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    @NotBlank(message = "商户号不能为空")
    private String merchantNo;

    private String merchantName;

    @NotNull(message = "代理层级不能为空")
    private Integer agentLevel;

    @NotNull(message = "分润比例不能为空")
    private BigDecimal commissionRate;

    private BigDecimal minCommission;

    private Integer settleType;

    private Integer status;

    private String remark;
}
