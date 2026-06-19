package com.payhub.settlement.verify;

import lombok.Data;

import java.io.Serializable;

@Data
public class BankCardVerifyResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean success;

    private String failCode;

    private String failReason;

    private String requestData;

    private String responseData;
}
