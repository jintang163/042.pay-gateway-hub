package com.payhub.invoice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("pay_invoice_callback_log")
public class InvoiceCallbackLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String invoiceNo;

    private String channelCode;

    private String channelInvoiceNo;

    private String requestBody;

    private String responseBody;

    private String notifyStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
