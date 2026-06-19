package com.payhub.risk.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.annotation.SandboxMode;
import com.payhub.common.result.Result;
import com.payhub.common.sandbox.SandboxDataCleanService;
import com.payhub.risk.dto.SandboxTestRequest;
import com.payhub.risk.dto.SandboxTestResultVO;
import com.payhub.risk.service.SandboxTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sandbox")
public class SandboxController {

    @Autowired
    private SandboxTestService sandboxTestService;

    @Autowired(required = false)
    private SandboxDataCleanService sandboxDataCleanService;

    @GetMapping("/status")
    public Result<Map<String, Object>> getSandboxStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", true);
        status.put("description", "沙箱环境已启用，可使用沙箱凭证模拟支付流程");
        status.put("dataRetentionDays", 1);
        status.put("cleanTime", "每天 04:00 自动清理沙箱数据");
        return Result.success(status);
    }

    @GetMapping("/test/scenes")
    public Result<List<Map<String, Object>>> listTestScenes() {
        List<Map<String, Object>> scenes = sandboxTestService.listTestScenes();
        return Result.success(scenes);
    }

    @PostMapping("/test")
    @SandboxMode
    public Result<SandboxTestResultVO> executeTest(@Valid @RequestBody SandboxTestRequest request) {
        log.info("执行沙箱测试, merchantNo={}, scene={}", request.getMerchantNo(), request.getTestScene());
        SandboxTestResultVO result = sandboxTestService.executeTest(request);
        return Result.success(result);
    }

    @GetMapping("/test/records")
    public Result<IPage<SandboxTestResultVO>> listTestRecords(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) String testScene,
            @RequestParam(required = false) Integer success) {
        Map<String, Object> params = new HashMap<>();
        params.put("merchantNo", merchantNo);
        params.put("testScene", testScene);
        params.put("success", success);
        IPage<SandboxTestResultVO> page = sandboxTestService.listTestRecords(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/test/records/{testId}")
    public Result<SandboxTestResultVO> getTestRecord(@PathVariable String testId) {
        SandboxTestResultVO record = sandboxTestService.getTestRecord(testId);
        return Result.success(record);
    }

    @PostMapping("/clear-history")
    public Result<Void> clearSandboxHistory() {
        log.info("手动清理沙箱历史数据");
        if (sandboxDataCleanService != null) {
            sandboxDataCleanService.cleanAllSandboxData();
        }
        return Result.success();
    }

    @GetMapping("/merchants")
    public Result<List<Map<String, Object>>> getSandboxMerchants() {
        List<Map<String, Object>> merchants = java.util.Arrays.asList(
                createMerchant("M000001", "沙箱测试商户1", "测试商户，仅用于沙箱环境"),
                createMerchant("M000002", "沙箱测试商户2", "测试商户，仅用于沙箱环境")
        );
        return Result.success(merchants);
    }

    @GetMapping("/pay-methods")
    public Result<List<Map<String, Object>>> getPayMethods() {
        List<Map<String, Object>> methods = java.util.Arrays.asList(
                createPayMethod("NATIVE", "扫码支付",
                        createChannel("WECHAT", "微信扫码"),
                        createChannel("ALIPAY", "支付宝扫码"),
                        createChannel("UNIONPAY", "银联扫码")),
                createPayMethod("H5", "H5支付",
                        createChannel("WECHAT", "微信H5"),
                        createChannel("ALIPAY", "支付宝H5")),
                createPayMethod("JSAPI", "公众号支付",
                        createChannel("WECHAT", "微信JSAPI"),
                        createChannel("ALIPAY", "支付宝服务窗")),
                createPayMethod("APP", "APP支付",
                        createChannel("WECHAT", "微信APP"),
                        createChannel("ALIPAY", "支付宝APP"))
        );
        return Result.success(methods);
    }

    private Map<String, Object> createMerchant(String id, String name, String description) {
        Map<String, Object> merchant = new HashMap<>();
        merchant.put("id", id);
        merchant.put("merchantName", name);
        merchant.put("description", description);
        return merchant;
    }

    private Map<String, Object> createPayMethod(String code, String name, Map<String, Object>... channels) {
        Map<String, Object> method = new HashMap<>();
        method.put("code", code);
        method.put("name", name);
        method.put("channels", java.util.Arrays.asList(channels));
        return method;
    }

    private Map<String, Object> createChannel(String code, String name) {
        Map<String, Object> channel = new HashMap<>();
        channel.put("code", code);
        channel.put("name", name);
        return channel;
    }
}
