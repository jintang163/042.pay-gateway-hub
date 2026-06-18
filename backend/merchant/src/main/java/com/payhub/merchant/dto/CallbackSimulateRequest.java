package com.payhub.merchant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class CallbackSimulateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "商户编号不能为空")
    private String merchantNo;

    private String callbackUrl;

    @NotBlank(message = "回调类型不能为空")
    private String callbackType;

    @NotBlank(message = "模拟状态不能为空")
    private String simulateStatus;

    private String signType;

    private String orderNo;

    private Long amount;

    private String customRequestBody;

    private String remark;
}
