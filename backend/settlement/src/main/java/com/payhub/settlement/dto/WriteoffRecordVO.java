package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WriteoffRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String writeoffNo;

    private String reconcileNo;

    private Long detailId;

    private String detailNo;

    private String merchantNo;

    private String payChannel;

    private String payChannelDesc;

    private Integer diffType;

    private String diffTypeDesc;

    private BigDecimal diffAmount;

    private BigDecimal writeoffAmount;

    private Integer writeoffType;

    private String writeoffTypeDesc;

    private Integer writeoffSource;

    private String writeoffSourceDesc;

    private Long ruleId;

    private String ruleName;

    private Integer writeoffStatus;

    private String writeoffStatusDesc;

    private String errorOrderNo;

    private String orderNo;

    private String channelTradeNo;

    private String writeoffRemark;

    private LocalDateTime executeTime;

    private String executeResult;

    private String operatorId;

    private String operatorName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
