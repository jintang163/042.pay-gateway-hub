package com.payhub.merchant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Map;

@Data
public class SignCodeExampleRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "签名类型不能为空")
    private String signType;

    @NotBlank(message = "编程语言不能为空")
    private String language;

    private Map<String, Object> params;

    private String key;
}
