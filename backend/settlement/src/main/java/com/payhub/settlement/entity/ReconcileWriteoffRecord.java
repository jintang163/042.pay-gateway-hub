package com.payhub.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("reconcile_writeoff_record")
public class ReconcileWriteoffRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String writeoffNo;

    private String reconcileNo;

    private Long detailId;

    private String detailNo;

    private String merchantNo;

    private String payChannel;

    private Integer diffType;

    private BigDecimal diffAmount;

    private BigDecimal writeoffAmount;

    private Integer writeoffType;

    private Integer writeoffSource;

    private Long ruleId;

    private String ruleName;

    private Integer writeoffStatus;

    private String errorOrderNo;

    private String orderNo;

    private String channelTradeNo;

    private String writeoffRemark;

    private LocalDateTime executeTime;

    private String executeResult;

    private String operatorId;

    private String operatorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
