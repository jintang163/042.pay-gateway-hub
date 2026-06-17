package com.payhub.pay.dto;

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
public class PayConfigSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "商户号不能为空")
    private String merchantNo;

    @NotBlank(message = "支付渠道不能为空")
    private String payChannel;

    @NotBlank(message = "支付方式不能为空")
    private String payType;

    @NotBlank(message = "通道编码不能为空")
    private String channelCode;

    @NotNull(message = "费率不能为空")
    private BigDecimal feeRate;

    private BigDecimal minFee;

    private BigDecimal maxFee;

    private Integer status;

    private Integer priority;

    private String whitelistIps;

    private String remark;
}
