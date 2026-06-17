package com.payhub.merchant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class MerchantAuditRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "商户编号不能为空")
    private String merchantNo;

    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    private String auditRemark;
}
