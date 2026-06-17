package com.payhub.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAuditRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long riskLogId;

    private String auditResult;

    private String auditRemark;
}
