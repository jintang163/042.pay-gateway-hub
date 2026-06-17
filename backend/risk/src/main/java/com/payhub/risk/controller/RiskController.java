package com.payhub.risk.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.risk.dto.RiskCheckRequest;
import com.payhub.risk.dto.RiskCheckResult;
import com.payhub.risk.dto.RiskLogVO;
import com.payhub.risk.service.RiskControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/risk")
public class RiskController {

    @Autowired
    private RiskControlService riskControlService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = riskControlService.getDashboardStats();
        return Result.success(stats);
    }

    @PostMapping("/check")
    public Result<RiskCheckResult> checkRisk(@Valid @RequestBody RiskCheckRequest request) {
        RiskCheckResult result = riskControlService.checkRisk(request);
        return Result.success(result);
    }

    @GetMapping("/logs")
    public Result<IPage<RiskLogVO>> listRiskLogs(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) String riskType,
            @RequestParam(required = false) Integer riskLevel,
            @RequestParam(required = false) String clientIp) {
        Map<String, Object> params = new HashMap<>();
        params.put("merchantNo", merchantNo);
        params.put("riskType", riskType);
        params.put("riskLevel", riskLevel);
        params.put("clientIp", clientIp);
        IPage<RiskLogVO> page = riskControlService.listRiskLogs(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/logs/page")
    public Result<IPage<RiskLogVO>> listRiskLogsPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String riskType,
            @RequestParam(required = false) Integer riskLevel,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) String clientIp,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Map<String, Object> params = new HashMap<>();
        params.put("merchantNo", merchantNo);
        params.put("orderNo", orderNo);
        params.put("riskType", riskType);
        params.put("riskLevel", riskLevel);
        params.put("actionType", actionType);
        params.put("auditStatus", auditStatus);
        params.put("clientIp", clientIp);
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        IPage<RiskLogVO> page = riskControlService.listRiskLogs(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/logs/{id}")
    public Result<RiskLogVO> getRiskLogById(@PathVariable Long id) {
        RiskLogVO log = riskControlService.getRiskLogById(id);
        return Result.success(log);
    }

    @PostMapping("/blacklist/ip/add")
    public Result<Void> addIpToBlacklist(@RequestParam String ip, @RequestParam(required = false) String reason) {
        riskControlService.addIpToBlacklist(ip, reason);
        return Result.success();
    }

    @PostMapping("/blacklist/ip/remove")
    public Result<Void> removeIpFromBlacklist(@RequestParam String ip) {
        riskControlService.removeIpFromBlacklist(ip);
        return Result.success();
    }

    @GetMapping("/blacklist/ip/check")
    public Result<Boolean> checkIpInBlacklist(@RequestParam String ip) {
        boolean inBlacklist = riskControlService.isIpInBlacklist(ip);
        return Result.success(inBlacklist);
    }
}
