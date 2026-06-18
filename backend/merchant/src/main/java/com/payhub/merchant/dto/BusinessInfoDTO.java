package com.payhub.merchant.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BusinessInfoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String businessLicenseNo;

    private String merchantName;

    private String legalPersonName;

    private String legalPersonIdNo;

    private String registeredCapital;

    private String establishmentDate;

    private String businessScope;

    private String registeredAddress;

    private String enterpriseType;

    private String businessStatus;

    private BigDecimal matchScore;

    private String matchScores;

    private String verifyRequestId;

    private String verifyVendor;

    private String verifySource;

    private LocalDateTime verifyTime;

    private String verifyRawRequest;

    private String verifyRawResponse;

    private Boolean fallbackUsed;
}
