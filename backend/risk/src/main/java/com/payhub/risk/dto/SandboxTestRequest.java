package com.payhub.risk.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxTestRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "商户号不能为空")
    private String merchantNo;

    @NotBlank(message = "测试场景不能为空")
    private String testScene;

    @NotBlank(message = "测试名称不能为空")
    private String testName;

    @NotBlank(message = "支付渠道不能为空")
    private String payChannel;

    @NotBlank(message = "支付方式不能为空")
    private String payType;

    @NotNull(message = "支付金额不能为空")
    private BigDecimal payAmount;

    private String notifyUrl;

    private String extraParams;
}
