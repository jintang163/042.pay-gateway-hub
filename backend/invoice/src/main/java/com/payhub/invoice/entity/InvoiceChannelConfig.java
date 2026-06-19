package com.payhub.invoice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("pay_invoice_channel_config")
public class InvoiceChannelConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String merchantNo;

    private String channelCode;

    private String appId;

    private String appSecret;

    private String accessToken;

    private String taxNum;

    private String companyName;

    private String companyAddress;

    private String companyPhone;

    private String bankName;

    private String bankAccount;

    private Integer enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
