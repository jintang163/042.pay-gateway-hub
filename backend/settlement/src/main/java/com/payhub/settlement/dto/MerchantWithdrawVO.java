package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MerchantWithdrawVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String withdrawNo;

    private String merchantNo;

    private String merchantName;

    private BigDecimal withdrawAmount;

    private BigDecimal actualAmount;

    private BigDecimal feeAmount;

    private Integer withdrawType;

    private String withdrawTypeDesc;

    private Integer withdrawStatus;

    private String withdrawStatusDesc;

    private String bankName;

    private String bankAccount;

    private String accountName;

    private String auditUser;

    private LocalDateTime auditTime;

    private String auditRemark;

    private String transferNo;

    private String transferChannel;

    private String channelTransferNo;

    private LocalDateTime transferTime;

    private String transferFailReason;

    private LocalDateTime arriveTime;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
