package com.payhub.merchant.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CallbackSimulateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String logNo;

    private String merchantNo;

    private String merchantName;

    private String orderNo;

    private String callbackUrl;

    private String callbackType;

    private String simulateStatus;

    private String signType;

    private String requestHeaders;

    private String requestBody;

    private Integer responseHttpStatus;

    private String responseBody;

    private Integer responseTimeMs;

    private Integer callbackStatus;

    private String callbackStatusDesc;

    private Integer retryCount;

    private String operatorName;

    private String remark;

    private String createdAt;
}
