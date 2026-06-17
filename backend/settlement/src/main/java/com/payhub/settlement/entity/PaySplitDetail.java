package com.payhub.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pay_split_detail")
public class PaySplitDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String splitDetailNo;

    private Long settlementId;

    private String settlementNo;

    private String orderNo;

    private String merchantNo;

    private String ruleNo;

    private String receiverAccount;

    private String receiverName;

    private String splitType;

    private BigDecimal splitValue;

    private BigDecimal splitAmount;

    private Integer status;

    private Integer transferStatus;

    private String transferNo;

    private String channelTransferNo;

    private String transferFailReason;

    private LocalDateTime transferTime;

    private Integer transferRetryCount;

    private LocalDateTime nextTransferRetryTime;

    private String transferChannel;

    private LocalDateTime settleTime;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
