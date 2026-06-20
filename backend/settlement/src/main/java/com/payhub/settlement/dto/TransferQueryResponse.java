package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransferQueryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String transferNo;

    private String channelTransferNo;

    private String channel;

    private String status;

    private String statusDesc;

    private String receiverAccount;

    private String receiverName;

    private BigDecimal amountFen;

    private BigDecimal amountYuan;

    private String failCode;

    private String failReason;

    private LocalDateTime transferTime;

    private String sourceType;

    private String sourceNo;

    private BigDecimal feeAmount;

    private Integer retryCount;
}
