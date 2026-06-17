package com.payhub.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("risk_control_log")
public class RiskControlLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String merchantNo;

    private String orderNo;

    private String riskType;

    private Integer riskLevel;

    private String riskRule;

    private String riskDesc;

    private String clientIp;

    private String userIdentity;

    private BigDecimal payAmount;

    private String payChannel;

    private String requestParams;

    private Integer handleResult;

    private String handleDesc;

    private LocalDateTime triggerTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
