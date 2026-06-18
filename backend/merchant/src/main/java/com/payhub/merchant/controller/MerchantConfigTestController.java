package com.payhub.merchant.controller;

import com.payhub.common.result.Result;
import com.payhub.merchant.dto.MerchantConfigTestReport;
import com.payhub.merchant.dto.MerchantConfigTestRequest;
import com.payhub.merchant.service.MerchantConfigTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/merchant-config")
public class MerchantConfigTestController {

    @Autowired
    private MerchantConfigTestService merchantConfigTestService;

    @PostMapping("/test")
    public Result<MerchantConfigTestReport> testConfig(@Valid @RequestBody MerchantConfigTestRequest request) {
        MerchantConfigTestReport report = merchantConfigTestService.runTest(request);
        return Result.success(report);
    }

    @GetMapping("/test/{merchantNo}")
    public Result<MerchantConfigTestReport> quickTest(@PathVariable String merchantNo) {
        MerchantConfigTestRequest request = new MerchantConfigTestRequest();
        request.setMerchantNo(merchantNo);
        MerchantConfigTestReport report = merchantConfigTestService.runTest(request);
        return Result.success(report);
    }
}
