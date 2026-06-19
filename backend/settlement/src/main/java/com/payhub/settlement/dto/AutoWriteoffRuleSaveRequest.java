package com.payhub.settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoWriteoffRuleSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String ruleName;

    private String merchantNo;

    private String payChannel;

    private Integer diffType;

    private BigDecimal maxAmount;

    private Integer autoWriteoff;

    private Integer handleType;

    private Integer enabled;

    private Integer priority;

    private String remark;
}
