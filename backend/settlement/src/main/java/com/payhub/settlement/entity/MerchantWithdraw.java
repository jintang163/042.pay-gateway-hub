package com.payhub.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("merchant_withdraw")
public class MerchantWithdraw implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String withdrawNo;

    private String merchantNo;

    private String merchantName;

    private BigDecimal withdrawAmount;

    private BigDecimal actualAmount;

    private BigDecimal feeAmount;

    private Integer withdrawType;

    private Integer withdrawStatus;

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

    private Integer transferRetryCount;

    private LocalDateTime nextTransferRetryTime;

    private LocalDateTime arriveTime;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
