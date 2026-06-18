package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ErrorOrderAuditRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long errorOrderId;

    private Integer auditStatus;

    private String auditRemark;
}
