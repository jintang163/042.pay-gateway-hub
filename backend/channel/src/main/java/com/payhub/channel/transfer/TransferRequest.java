package com.payhub.channel.transfer;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class TransferRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String transferNo;

    private String channel;

    private String receiverAccount;

    private String receiverName;

    private BigDecimal amount;

    private String bankName;

    private String bankCode;

    private String bankBranchName;

    private String bankBranchCode;

    private Integer receiverType;

    private String idCardType;

    private String idCardNo;

    private String idCardName;

    private String bankPhone;

    private String purpose;

    private String remark;

    private String sourceType;

    private String sourceNo;

    private String merchantNo;

    private String notifyUrl;
}
