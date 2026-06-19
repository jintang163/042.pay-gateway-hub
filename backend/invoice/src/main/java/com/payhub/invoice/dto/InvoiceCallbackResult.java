package com.payhub.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCallbackResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String invoiceNo;

    private String channelInvoiceNo;

    private String channelCode;

    private Integer invoiceStatus;

    private String pdfUrl;

    private BigDecimal invoiceAmount;

    private BigDecimal taxAmount;

    private BigDecimal totalAmount;

    private LocalDateTime issueTime;

    private String failReason;
}
