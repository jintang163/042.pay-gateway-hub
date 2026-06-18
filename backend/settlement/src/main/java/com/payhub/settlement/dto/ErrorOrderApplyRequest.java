package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ErrorOrderApplyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long reconcileDetailId;

    private Integer handleType;

    private String applyRemark;

    private BigDecimal adjustAmount;
}
