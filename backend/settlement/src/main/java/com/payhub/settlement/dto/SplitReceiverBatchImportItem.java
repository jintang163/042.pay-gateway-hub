package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SplitReceiverBatchImportItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String receiverName;

    private Integer receiverType;

    private String idCardNo;

    private String idCardName;

    private String bankCardNo;

    private String bankPhone;

    private String bankName;

    private String bankBranchName;

    private String contactName;

    private String contactPhone;

    private String contactEmail;

    private String remark;
}
