package com.payhub.merchant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.merchant.dto.*;
import com.payhub.merchant.service.MerchantInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
public class MerchantInfoController {

    @Autowired
    private MerchantInfoService merchantInfoService;

    @PostMapping("/apply")
    public Result<String> apply(@Valid @RequestBody MerchantApplyRequest request) {
        String merchantNo = merchantInfoService.apply(request);
        return Result.success(merchantNo);
    }

    @PostMapping("/audit")
    public Result<Void> audit(@Valid @RequestBody MerchantAuditRequest request) {
        merchantInfoService.audit(request);
        return Result.success();
    }

    @GetMapping("/{merchantNo}")
    public Result<MerchantVO> getByMerchantNo(@PathVariable String merchantNo) {
        MerchantVO merchant = merchantInfoService.getByMerchantNo(merchantNo);
        return Result.success(merchant);
    }

    @GetMapping("/list")
    public Result<IPage<MerchantVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String merchantName,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) Integer status) {
        Map<String, Object> params = new HashMap<>();
        params.put("merchantName", merchantName);
        params.put("merchantNo", merchantNo);
        params.put("auditStatus", auditStatus);
        params.put("status", status);
        IPage<MerchantVO> page = merchantInfoService.listPage(current, size, params);
        return Result.success(page);
    }

    @PostMapping("/resetApiKey")
    public Result<ApiKeyVO> resetApiKey(@Valid @RequestBody ApiKeyResetRequest request) {
        ApiKeyVO apiKeyVO = merchantInfoService.resetApiKey(request);
        return Result.success(apiKeyVO);
    }

    @PostMapping("/testCallback")
    public Result<Boolean> testCallback(@Valid @RequestBody CallbackTestRequest request) {
        boolean result = merchantInfoService.testCallback(request.getMerchantNo(), request.getCallbackUrl());
        return Result.success(result);
    }

    @GetMapping("/{merchantNo}/audit-progress")
    public Result<AuditProgressVO> getAuditProgress(@PathVariable String merchantNo) {
        AuditProgressVO progress = merchantInfoService.getAuditProgress(merchantNo);
        return Result.success(progress);
    }
}
