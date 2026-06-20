package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class MerchantBalanceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String merchantNo;

    private BigDecimal totalSettleAmount;

    private BigDecimal totalWithdrawAmount;

    private BigDecimal availableBalance;

    private BigDecimal pendingWithdrawAmount;

    private BigDecimal t1Balance;

    private BigDecimal instantBalance;

    private BigDecimal minWithdrawAmount;

    private BigDecimal maxWithdrawAmount;

    private BigDecimal instantFeeRate;

    private BigDecimal auditThreshold;
}
