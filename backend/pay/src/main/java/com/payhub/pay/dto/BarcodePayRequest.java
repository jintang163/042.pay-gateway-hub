package com.payhub.pay.dto;

import com.payhub.common.dto.SignBaseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BarcodePayRequest extends SignBaseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "商户订单号不能为空")
    private String merchantOrderNo;

    @NotNull(message = "支付金额不能为空")
    private BigDecimal payAmount;

    @NotBlank(message = "支付渠道不能为空")
    private String payChannel;

    @NotBlank(message = "付款码不能为空")
    private String authCode;

    private String scene;

    @NotBlank(message = "商品标题不能为空")
    private String productSubject;

    private String productDetail;

    @NotBlank(message = "异步通知地址不能为空")
    private String notifyUrl;

    private String extraParams;

    private String clientIp;

    private String couponCode;

    private String activityCode;
}
