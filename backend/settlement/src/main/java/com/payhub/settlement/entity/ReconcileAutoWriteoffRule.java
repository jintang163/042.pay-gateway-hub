package com.payhub.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("reconcile_auto_writeoff_rule")
public class ReconcileAutoWriteoffRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleName;

    private String merchantNo;

    private String payChannel;

    private Integer diffType;

    private BigDecimal maxAmount;

    private Integer autoWriteoff;

    private Integer handleType;

    private Integer enabled;

    private Integer priority;

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
