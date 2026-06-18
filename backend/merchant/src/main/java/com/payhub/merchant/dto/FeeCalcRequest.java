package com.payhub.merchant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class FeeCalcRequest {

    @NotBlank(message = "行业编码不能为空")
    private String industryCode;

    private String payChannel;

    @NotNull(message = "交易金额不能为空")
    private BigDecimal amount;

    private String merchantNo;
}
