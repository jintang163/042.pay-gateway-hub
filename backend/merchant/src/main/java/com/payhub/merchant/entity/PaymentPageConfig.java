package com.payhub.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("payment_page_config")
public class PaymentPageConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String merchantNo;

    private String pageTitle;

    private String logoUrl;

    private String primaryColor;

    private String secondaryColor;

    private String backgroundColor;

    private String textColor;

    private String buttonColor;

    private String buttonTextColor;

    private String templateCode;

    private String customCss;

    private String footerText;

    private String returnUrl;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
