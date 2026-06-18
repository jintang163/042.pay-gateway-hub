package com.payhub.merchant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class PaymentPageConfigSaveRequest {

    @NotBlank(message = "商户号不能为空")
    private String merchantNo;

    private String pageTitle;

    private String logoUrl;

    private String primaryColor;

    private String secondaryColor;

    private String backgroundColor;

    private String textColor;

    private String buttonColor;

    private String buttonTextColor;

    private String colorSchemeCode;

    private String customCss;

    private String footerText;

    private String returnUrl;

    private Integer status;
}
