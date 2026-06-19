package com.payhub.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String invoiceNo;

    private String merchantNo;

    private String orderNo;

    private String channelInvoiceNo;

    private String channelCode;

    private Integer invoiceType;

    private Integer invoiceStatus;

    private String invoiceStatusDesc;

    private Integer titleType;

    private String titleTypeDesc;

    private String buyerTitle;

    private String buyerTaxNo;

    private String buyerAddress;

    private String buyerBankName;

    private String buyerBankAccount;

    private String buyerPhone;

    private String buyerEmail;

    private String invoiceContent;

    private BigDecimal invoiceAmount;

    private BigDecimal taxAmount;

    private BigDecimal totalAmount;

    private String taxRate;

    private String pdfUrl;

    private String originalInvoiceNo;

    private String redReason;

    private String remark;

    private String failReason;

    private LocalDateTime issueTime;

    private LocalDateTime createdAt;

    private List<InvoiceItemDTO> items;
}
