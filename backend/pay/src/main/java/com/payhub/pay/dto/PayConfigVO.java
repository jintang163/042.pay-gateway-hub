package com.payhub.pay.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PayConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String merchantNo;

    private String payChannel;

    private String payType;

    private String channelCode;

    private BigDecimal feeRate;

    private BigDecimal minFee;

    private BigDecimal maxFee;

    private Integer status;

    private String statusDesc;

    private Integer priority;

    private String whitelistIps;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
