package com.payhub.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("split_receiver_verify_log")
public class SplitReceiverVerifyLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String logNo;

    private String merchantNo;

    private String receiverNo;

    private Integer verifyChannel;

    private String verifyRequestId;

    private String idCardName;

    private String idCardNo;

    private String bankCardNo;

    private String bankPhone;

    private Integer verifyStatus;

    private String verifyResult;

    private String verifyFailCode;

    private String verifyFailReason;

    private LocalDateTime verifyTime;

    private String requestData;

    private String responseData;

    private String operatorId;

    private String operatorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
