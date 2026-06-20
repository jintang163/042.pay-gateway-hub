package com.payhub.channel.transfer;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransferResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;

    private String transferNo;

    private String channelTransferNo;

    private String status;

    private String failReason;

    private String failCode;

    private BigDecimal feeAmount;

    private LocalDateTime completeTime;

    private String responseData;

    private String requestData;

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAIL = "FAIL";
    public static final String STATUS_PROCESSING = "PROCESSING";

    public static TransferResult success(String transferNo, String channelTransferNo, LocalDateTime completeTime) {
        TransferResult result = new TransferResult();
        result.setSuccess(true);
        result.setTransferNo(transferNo);
        result.setChannelTransferNo(channelTransferNo);
        result.setStatus(STATUS_SUCCESS);
        result.setCompleteTime(completeTime);
        return result;
    }

    public static TransferResult fail(String transferNo, String failReason) {
        TransferResult result = new TransferResult();
        result.setSuccess(false);
        result.setTransferNo(transferNo);
        result.setStatus(STATUS_FAIL);
        result.setFailReason(failReason);
        result.setCompleteTime(LocalDateTime.now());
        return result;
    }

    public static TransferResult fail(String transferNo, String failCode, String failReason) {
        TransferResult result = fail(transferNo, failReason);
        result.setFailCode(failCode);
        return result;
    }

    public static TransferResult processing(String transferNo, String channelTransferNo) {
        TransferResult result = new TransferResult();
        result.setSuccess(false);
        result.setTransferNo(transferNo);
        result.setChannelTransferNo(channelTransferNo);
        result.setStatus(STATUS_PROCESSING);
        return result;
    }
}
