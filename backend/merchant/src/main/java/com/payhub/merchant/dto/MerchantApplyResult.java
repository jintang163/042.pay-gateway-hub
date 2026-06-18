package com.payhub.merchant.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class MerchantApplyResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String merchantNo;

    private String merchantName;

    private Integer auditStep;

    private String auditStepName;

    private Integer auditStatus;

    private String auditStatusDesc;
}
