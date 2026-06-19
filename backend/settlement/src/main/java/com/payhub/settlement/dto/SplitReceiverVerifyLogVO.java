package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SplitReceiverVerifyLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String logNo;

    private String merchantNo;

    private String receiverNo;

    private Integer verifyChannel;

    private String verifyChannelDesc;

    private String verifyRequestId;

    private String idCardName;

    private String idCardNo;

    private String bankCardNo;

    private String bankPhone;

    private Integer verifyStatus;

    private String verifyStatusDesc;

    private String verifyResult;

    private String verifyFailCode;

    private String verifyFailReason;

    private LocalDateTime verifyTime;

    private String requestData;

    private String responseData;

    private String operatorId;

    private String operatorName;

    private LocalDateTime createdAt;
}
