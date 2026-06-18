package com.payhub.settlement.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AgentRelationSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "商户号不能为空")
    private String merchantNo;

    private String merchantName;

    private String parentMerchantNo;

    private String parentMerchantName;

    @NotNull(message = "代理层级不能为空")
    private Integer agentLevel;

    private BigDecimal commissionRate;

    private Integer status;

    private String remark;
}
