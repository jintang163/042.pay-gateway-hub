package com.payhub.risk.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.risk.dto.RiskDeviceVO;
import com.payhub.risk.service.RiskDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/risk/devices")
public class RiskDeviceController {

    @Autowired
    private RiskDeviceService riskDeviceService;

    @GetMapping("/page")
    public Result<IPage<RiskDeviceVO>> listPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String userIdentity,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer riskScoreMin) {
        Map<String, Object> params = new HashMap<>();
        params.put("deviceId", deviceId);
        params.put("userIdentity", userIdentity);
        params.put("merchantNo", merchantNo);
        params.put("status", status);
        params.put("riskScoreMin", riskScoreMin);
        IPage<RiskDeviceVO> page = riskDeviceService.listPage(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/{deviceId}")
    public Result<RiskDeviceVO> getByDeviceId(@PathVariable String deviceId) {
        RiskDeviceVO device = riskDeviceService.getByDeviceId(deviceId);
        return Result.success(device);
    }

    @PostMapping("/{deviceId}/risk")
    public Result<Void> updateRiskScore(
            @PathVariable String deviceId,
            @RequestParam Integer scoreDelta,
            @RequestParam(required = false) String tag) {
        riskDeviceService.updateRiskScore(deviceId, scoreDelta, tag);
        return Result.success();
    }

    @PostMapping("/{deviceId}/mark-abnormal")
    public Result<Void> markAbnormal(
            @PathVariable String deviceId,
            @RequestParam(required = false) String reason) {
        riskDeviceService.markAbnormal(deviceId, reason);
        return Result.success();
    }

    @PostMapping("/{deviceId}/mark-normal")
    public Result<Void> markNormal(@PathVariable String deviceId) {
        riskDeviceService.markNormal(deviceId);
        return Result.success();
    }

    @PostMapping("/register")
    public Result<RiskDeviceVO> registerDevice(
            @RequestParam String deviceId,
            @RequestBody(required = false) Map<String, Object> deviceInfo) {
        RiskDeviceVO device = riskDeviceService.registerDevice(deviceId, deviceInfo);
        return Result.success(device);
    }
}
