package com.payhub.settlement.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class MerchantWithdrawApplyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "商户号不能为空")
    private String merchantNo;

    @NotNull(message = "提现金额不能为空")
    private BigDecimal withdrawAmount;

    @NotNull(message = "提现类型不能为空")
    private Integer withdrawType;

    @NotBlank(message = "开户银行不能为空")
    private String bankName;

    @NotBlank(message = "银行账号不能为空")
    private String bankAccount;

    @NotBlank(message = "开户名不能为空")
    private String accountName;

    private String remark;
}
