package com.payhub.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("error_order")
public class ErrorOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String errorNo;

    private String reconcileNo;

    private Long reconcileDetailId;

    private String payChannel;

    private Integer errorType;

    private Integer handleType;

    private String orderNo;

    private String merchantNo;

    private String channelTradeNo;

    private BigDecimal orderAmount;

    private BigDecimal actualAmount;

    private BigDecimal diffAmount;

    private Integer errorStatus;

    private String applyUserId;

    private String applyUserName;

    private LocalDateTime applyTime;

    private String applyRemark;

    private String auditUserId;

    private String auditUserName;

    private LocalDateTime auditTime;

    private String auditRemark;

    private Integer auditStatus;

    private String handleUserId;

    private String handleUserName;

    private LocalDateTime handleTime;

    private String handleResult;

    private String refundNo;

    private String newOrderNo;

    private String extraInfo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
