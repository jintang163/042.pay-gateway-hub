package com.payhub.risk.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.risk.dto.RiskAuditRequest;
import com.payhub.risk.dto.SmsVerifyRequest;
import com.payhub.risk.entity.RiskAuditRecord;

import java.util.Map;

public interface RiskAuditService extends IService<RiskAuditRecord> {

    RiskAuditRecord createAudit(Long riskLogId, String auditType, Integer auditLevel);

    RiskAuditRecord audit(RiskAuditRequest request, String auditUserId, String auditUserName);

    IPage<RiskAuditRecord> listPage(Long current, Long size, Map<String, Object> params);

    RiskAuditRecord getByRiskLogId(Long riskLogId);

    String sendSmsCode(Long auditId, String mobile);

    boolean verifySmsCode(SmsVerifyRequest request);
}
