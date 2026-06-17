package com.payhub.risk.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.risk.dto.RiskAuditRequest;
import com.payhub.risk.dto.SmsVerifyRequest;
import com.payhub.risk.entity.RiskAuditRecord;
import com.payhub.risk.service.RiskAuditService;
import com.payhub.risk.service.RiskControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/risk/audits")
public class RiskAuditController {

    @Autowired
    private RiskAuditService riskAuditService;

    @Autowired
    private RiskControlService riskControlService;

    @GetMapping("/page")
    public Result<IPage<RiskAuditRecord>> listPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) String auditType,
            @RequestParam(required = false) String merchantNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("auditStatus", auditStatus);
        params.put("auditType", auditType);
        params.put("merchantNo", merchantNo);
        IPage<RiskAuditRecord> page = riskAuditService.listPage(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result<RiskAuditRecord> getById(@PathVariable Long id) {
        RiskAuditRecord record = riskAuditService.getById(id);
        return Result.success(record);
    }

    @PostMapping("/audit")
    public Result<Void> audit(@Valid @RequestBody RiskAuditRequest request) {
        String auditUserId = "mock_user_001";
        String auditUserName = "模拟审核员";
        riskAuditService.audit(request, auditUserId, auditUserName);
        return Result.success();
    }

    @PostMapping("/{id}/sms/send")
    public Result<Void> sendSms(
            @PathVariable Long id,
            @RequestParam String mobile) {
        riskAuditService.sendSmsCode(id, mobile);
        return Result.success();
    }

    @PostMapping("/sms/verify")
    public Result<Boolean> verifySms(@Valid @RequestBody SmsVerifyRequest request) {
        boolean verified = riskAuditService.verifySms(request);
        return Result.success(verified);
    }
}
