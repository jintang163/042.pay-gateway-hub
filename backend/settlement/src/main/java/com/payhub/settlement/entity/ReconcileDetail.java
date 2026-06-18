package com.payhub.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("reconcile_detail")
public class ReconcileDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String detailNo;

    private String reconcileNo;

    private LocalDate reconcileDate;

    private String payChannel;

    private Integer diffType;

    private String orderNo;

    private String merchantNo;

    private String channelTradeNo;

    private BigDecimal localAmount;

    private BigDecimal channelAmount;

    private BigDecimal diffAmount;

    private Integer localStatus;

    private String channelStatus;

    private LocalDateTime localPayTime;

    private LocalDateTime channelPayTime;

    private String errorOrderNo;

    private Integer handleStatus;

    private String handleRemark;

    private String handleUserId;

    private String handleUserName;

    private LocalDateTime handleTime;

    private String extraInfo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
