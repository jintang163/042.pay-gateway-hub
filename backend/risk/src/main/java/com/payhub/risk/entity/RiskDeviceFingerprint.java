package com.payhub.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("risk_device_fingerprint")
public class RiskDeviceFingerprint implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
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

    private String extraInfo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
