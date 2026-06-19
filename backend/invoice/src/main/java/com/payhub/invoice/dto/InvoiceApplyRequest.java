package com.payhub.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceApplyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String merchantNo;

    @NotBlank(message = "订单号不能为空")
    private String orderNo;

    private String channelCode;

    @NotNull(message = "抬头类型不能为空")
    private Integer titleType;

    @NotBlank(message = "购方抬头不能为空")
    private String buyerTitle;

    private String buyerTaxNo;

    private String buyerAddress;

    private String buyerBankName;

    private String buyerBankAccount;

    private String buyerPhone;

    private String buyerEmail;

    private String invoiceContent;

    private BigDecimal invoiceAmount;

    private String remark;

    private String notifyUrl;

    private List<InvoiceItemDTO> items;
}
