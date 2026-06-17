package com.payhub.merchant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class CallbackTestRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "商户编号不能为空")
    private String merchantNo;

    @NotBlank(message = "回调地址不能为空")
    private String callbackUrl;
}
