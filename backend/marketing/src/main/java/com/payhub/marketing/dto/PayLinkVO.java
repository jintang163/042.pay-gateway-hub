package com.payhub.marketing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PayLinkVO {

    private Long id;

    private String linkCode;

    private String merchantNo;

    private String title;

    private BigDecimal fixedAmount;

    private Boolean amountEditable;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private String payChannel;

    private String productSubject;

    private String productDetail;

    private String notifyUrl;

    private String redirectUrl;

    private LocalDateTime expireTime;

    private Boolean singleUse;

    private Integer maxUseCount;

    private Integer usedCount;

    private Integer status;

    private String statusDesc;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
