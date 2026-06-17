package com.payhub.merchant.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class MerchantApplyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "商户名称不能为空")
    private String merchantName;

    @NotBlank(message = "营业执照号不能为空")
    private String businessLicenseNo;

    @NotBlank(message = "法人姓名不能为空")
    private String legalPersonName;

    @NotBlank(message = "法人身份证不能为空")
    private String legalPersonIdNo;

    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;

    @Email(message = "邮箱格式不正确")
    private String contactEmail;

    @NotBlank(message = "结算银行不能为空")
    private String settlementBankName;

    @NotBlank(message = "银行账号不能为空")
    private String settlementBankAccount;

    @NotBlank(message = "开户名不能为空")
    private String settlementAccountName;
}
