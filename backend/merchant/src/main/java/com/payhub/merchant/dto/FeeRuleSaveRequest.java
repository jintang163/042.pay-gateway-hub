package com.payhub.merchant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class FeeRuleSaveRequest {

    private Long id;

    @NotBlank(message = "行业编码不能为空")
    private String industryCode;

    @NotBlank(message = "行业名称不能为空")
    private String industryName;

    private String payChannel;

    @NotNull(message = "金额区间最小值不能为空")
    private BigDecimal minAmount;

    @NotNull(message = "金额区间最大值不能为空")
    private BigDecimal maxAmount;

    @NotNull(message = "费率不能为空")
    private BigDecimal feeRate;

    private BigDecimal minFee;

    private BigDecimal maxFee;

    private Integer priority;

    private Integer status;

    private String remark;
}
