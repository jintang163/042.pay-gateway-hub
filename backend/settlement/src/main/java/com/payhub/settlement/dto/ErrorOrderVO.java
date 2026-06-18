package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ErrorOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String errorNo;

    private String reconcileNo;

    private Long reconcileDetailId;

    private String payChannel;

    private String payChannelDesc;

    private Integer errorType;

    private String errorTypeDesc;

    private Integer handleType;

    private String handleTypeDesc;

    private String orderNo;

    private String merchantNo;

    private String channelTradeNo;

    private BigDecimal orderAmount;

    private BigDecimal actualAmount;

    private BigDecimal diffAmount;

    private Integer errorStatus;

    private String errorStatusDesc;

    private String applyUserId;

    private String applyUserName;

    private LocalDateTime applyTime;

    private String applyRemark;

    private String auditUserId;

    private String auditUserName;

    private LocalDateTime auditTime;

    private String auditRemark;

    private Integer auditStatus;

    private String auditStatusDesc;

    private String handleUserId;

    private String handleUserName;

    private LocalDateTime handleTime;

    private String handleResult;

    private String refundNo;

    private String newOrderNo;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
