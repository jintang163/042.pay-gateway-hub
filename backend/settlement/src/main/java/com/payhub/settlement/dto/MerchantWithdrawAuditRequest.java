package com.payhub.settlement.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class MerchantWithdrawAuditRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "提现ID不能为空")
    private Long id;

    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    private String auditRemark;

    @NotBlank(message = "审核人不能为空")
    private String auditUser;
}
