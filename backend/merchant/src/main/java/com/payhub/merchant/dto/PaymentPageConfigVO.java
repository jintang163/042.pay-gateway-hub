package com.payhub.merchant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentPageConfigVO {

    private Long id;

    private String merchantNo;

    private String merchantName;

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

    private String statusDesc;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String pageUrl;
}
