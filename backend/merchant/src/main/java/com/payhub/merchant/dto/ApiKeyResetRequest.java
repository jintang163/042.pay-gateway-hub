package com.payhub.merchant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class ApiKeyResetRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "商户编号不能为空")
    private String merchantNo;

    @NotBlank(message = "短信验证码不能为空")
    private String smsCode;

    @NotBlank(message = "签名类型不能为空")
    private String signType;
}
