package com.payhub.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("split_receiver")
public class SplitReceiver implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String receiverNo;

    private String merchantNo;

    private String receiverName;

    private Integer receiverType;

    private String idCardNo;

    private String idCardName;

    private String bankCardNo;

    private String bankPhone;

    private String bankName;

    private String bankBranchName;

    private Integer verifyStatus;

    private Integer verifyChannel;

    private LocalDateTime verifyTime;

    private String verifyFailReason;

    private String verifyFailCode;

    private String verifyRequestId;

    private Integer idCardVerifyChannel;

    private Integer idCardVerifyStatus;

    private LocalDateTime idCardVerifyTime;

    private String idCardVerifyRequestId;

    private String idCardVerifyLevel;

    private String idCardVerifyFailCode;

    private String idCardVerifyFailReason;

    private String contactName;

    private String contactPhone;

    private String contactEmail;

    private Integer status;

    private String remark;

    private String operatorId;

    private String operatorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
