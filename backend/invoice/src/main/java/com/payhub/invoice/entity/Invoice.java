package com.payhub.invoice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pay_invoice")
public class Invoice implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String invoiceNo;

    private String merchantNo;

    private String orderNo;

    private String channelInvoiceNo;

    private String channelCode;

    private Integer invoiceType;

    private Integer invoiceStatus;

    private Integer titleType;

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

    private String notifyUrl;

    private LocalDateTime issueTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
