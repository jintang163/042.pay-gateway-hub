package com.payhub.pay.dto;

import com.payhub.common.dto.SignBaseDTO;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RefundRequest extends SignBaseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    @NotBlank(message = "商户退款单号不能为空")
    private String merchantRefundNo;

    @NotNull(message = "退款金额不能为空")
    private BigDecimal refundAmount;

    private String refundReason;
}
