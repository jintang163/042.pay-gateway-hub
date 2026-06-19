package com.payhub.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("report_push_record")
public class ReportPushRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String recordNo;

    private String subscriptionNo;

    private String merchantNo;

    private Integer reportType;

    private String reportCategory;

    private String reportTitle;

    private String reportPeriod;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer pushStatus;

    private Integer pushChannel;

    private String emailTargets;

    private String phoneTargets;

    private String fileUrl;

    private Long fileSize;

    private Integer successCount;

    private Integer failCount;

    private String failReason;

    private Integer triggerType;

    private LocalDateTime pushTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
