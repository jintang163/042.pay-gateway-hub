package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ReportPushRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String recordNo;

    private String subscriptionNo;

    private String merchantNo;

    private Integer reportType;

    private String reportTypeDesc;

    private String reportCategory;

    private String reportTitle;

    private String reportPeriod;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer pushStatus;

    private String pushStatusDesc;

    private Integer pushChannel;

    private String pushChannelDesc;

    private String emailTargets;

    private String phoneTargets;

    private String fileUrl;

    private Long fileSize;

    private Integer successCount;

    private Integer failCount;

    private String failReason;

    private Integer triggerType;

    private String triggerTypeDesc;

    private LocalDateTime pushTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
