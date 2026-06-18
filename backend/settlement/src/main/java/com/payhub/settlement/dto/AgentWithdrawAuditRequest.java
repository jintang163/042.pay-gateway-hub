package com.payhub.settlement.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class AgentWithdrawAuditRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "提现ID不能为空")
    private Long id;

    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    private String auditRemark;

    private String auditUser;
}
