package com.payhub.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceRedFlushRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String merchantNo;

    @NotBlank(message = "原发票号不能为空")
    private String originalInvoiceNo;

    private String redReason;

    private String notifyUrl;
}
