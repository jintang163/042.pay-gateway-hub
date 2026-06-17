package com.payhub.risk.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class RiskDeviceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String deviceId;

    private String deviceType;

    private String osType;

    private String osVersion;

    private String browserType;

    private String browserVersion;

    private String appVersion;

    private String screenResolution;

    private String language;

    private String timezone;

    private String userAgent;

    private String userIdentity;

    private String merchantNo;

    private String firstSeenIp;

    private String lastSeenIp;

    private LocalDateTime firstSeenTime;

    private LocalDateTime lastSeenTime;

    private Long totalRequestCount;

    private Integer riskScore;

    private String riskTags;

    private Integer status;

    private String statusDesc;

    private String extraInfo;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
