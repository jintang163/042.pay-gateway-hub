package com.payhub.pay.dto.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PayOrderBriefVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private String merchantNo;

    private String merchantOrderNo;

    private BigDecimal payAmount;

    private String payChannel;

    private String payType;

    private Integer payStatus;

    private LocalDateTime expireTime;

    private LocalDateTime createdAt;
}
