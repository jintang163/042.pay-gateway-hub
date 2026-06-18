package com.payhub.merchant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.merchant.dto.PaymentPageConfigSaveRequest;
import com.payhub.merchant.dto.PaymentPageConfigVO;
import com.payhub.merchant.service.PaymentPageConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment-page")
public class PaymentPageConfigController {

    @Autowired
    private PaymentPageConfigService paymentPageConfigService;

    @PostMapping("/config")
    public Result<PaymentPageConfigVO> saveConfig(@Valid @RequestBody PaymentPageConfigSaveRequest request) {
        PaymentPageConfigVO result = paymentPageConfigService.saveConfig(request);
        return Result.success(result);
    }

    @GetMapping("/config/merchant/{merchantNo}")
    public Result<PaymentPageConfigVO> getByMerchantNo(@PathVariable String merchantNo) {
        PaymentPageConfigVO result = paymentPageConfigService.getByMerchantNo(merchantNo);
        return Result.success(result);
    }

    @GetMapping("/config/{id}")
    public Result<PaymentPageConfigVO> getById(@PathVariable Long id) {
        PaymentPageConfigVO result = paymentPageConfigService.getById(id);
        return Result.success(result);
    }

    @GetMapping("/config/list")
    public Result<IPage<PaymentPageConfigVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) String merchantName,
            @RequestParam(required = false) String templateCode,
            @RequestParam(required = false) Integer status) {
        Map<String, Object> params = new HashMap<>();
        params.put("merchantNo", merchantNo);
        params.put("merchantName", merchantName);
        params.put("templateCode", templateCode);
        params.put("status", status);
        IPage<PaymentPageConfigVO> page = paymentPageConfigService.listPage(current, size, params);
        return Result.success(page);
    }

    @PutMapping("/config/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        paymentPageConfigService.updateStatus(id, status);
        return Result.success();
    }

    @DeleteMapping("/config/{id}")
    public Result<Void> deleteConfig(@PathVariable Long id) {
        paymentPageConfigService.deleteConfig(id);
        return Result.success();
    }

    @GetMapping("/public/{merchantNo}")
    public Result<PaymentPageConfigVO> getPublicConfig(@PathVariable String merchantNo) {
        PaymentPageConfigVO result = paymentPageConfigService.getPublicConfig(merchantNo);
        return Result.success(result);
    }
}
