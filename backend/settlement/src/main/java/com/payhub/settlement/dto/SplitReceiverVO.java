package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SplitReceiverVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String receiverNo;

    private String merchantNo;

    private String receiverName;

    private Integer receiverType;

    private String receiverTypeDesc;

    private String idCardNo;

    private String idCardName;

    private String bankCardNo;

    private String bankPhone;

    private String bankName;

    private String bankBranchName;

    private Integer verifyStatus;

    private String verifyStatusDesc;

    private Integer verifyChannel;

    private String verifyChannelDesc;

    private LocalDateTime verifyTime;

    private String verifyFailReason;

    private String verifyRequestId;

    private String contactName;

    private String contactPhone;

    private String contactEmail;

    private Integer status;

    private String statusDesc;

    private String remark;

    private String operatorId;

    private String operatorName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
