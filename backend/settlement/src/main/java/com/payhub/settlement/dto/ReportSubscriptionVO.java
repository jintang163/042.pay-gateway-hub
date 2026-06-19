package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ReportSubscriptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String subscriptionNo;

    private String merchantNo;

    private Integer reportType;

    private String reportTypeDesc;

    private String reportCategory;

    private Integer pushChannel;

    private String pushChannelDesc;

    private String emailList;

    private String phoneList;

    private String pushTime;

    private Integer enabled;

    private String enabledDesc;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
