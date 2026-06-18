package com.payhub.merchant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class MerchantConfigTestRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "商户号不能为空")
    private String merchantNo;

    private String callbackUrl;

    private String signType;

    private String testAmount;
}
