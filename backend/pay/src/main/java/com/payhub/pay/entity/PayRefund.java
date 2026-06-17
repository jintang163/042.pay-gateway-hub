package com.payhub.pay.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pay_refund")
public class PayRefund implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String refundNo;

    private String orderNo;

    private String merchantNo;

    private String merchantRefundNo;

    private BigDecimal payAmount;

    private BigDecimal refundAmount;

    private String refundReason;

    private Integer refundStatus;

    private String channelRefundNo;

    private Integer retryCount;

    private LocalDateTime nextRetryTime;

    private LocalDateTime refundTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
