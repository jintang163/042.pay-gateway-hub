package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ReconcileDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String detailNo;

    private String reconcileNo;

    private LocalDate reconcileDate;

    private String payChannel;

    private String payChannelDesc;

    private Integer diffType;

    private String diffTypeDesc;

    private String orderNo;

    private String merchantNo;

    private String channelTradeNo;

    private BigDecimal localAmount;

    private BigDecimal channelAmount;

    private BigDecimal diffAmount;

    private Integer localStatus;

    private String localStatusDesc;

    private String channelStatus;

    private LocalDateTime localPayTime;

    private LocalDateTime channelPayTime;

    private String errorOrderNo;

    private Integer handleStatus;

    private String handleStatusDesc;

    private String handleRemark;

    private String handleUserId;

    private String handleUserName;

    private LocalDateTime handleTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
