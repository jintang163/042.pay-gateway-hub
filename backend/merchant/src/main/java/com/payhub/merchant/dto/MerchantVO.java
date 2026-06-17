package com.payhub.merchant.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MerchantVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String merchantNo;

    private String merchantName;

    private String businessLicenseNo;

    private String legalPersonName;

    private String contactPhone;

    private String contactEmail;

    private String settlementBankName;

    private String settlementBankAccount;

    private String settlementAccountName;

    private Integer auditStatus;

    private String auditStatusDesc;

    private String auditRemark;

    private Integer status;

    private String statusDesc;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
