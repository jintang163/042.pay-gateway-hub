package com.payhub.risk.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.risk.dto.RiskCheckRequest;
import com.payhub.risk.dto.RiskCheckResult;
import com.payhub.risk.dto.RiskLogVO;
import com.payhub.risk.service.RiskControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/risk")
public class RiskController {

    @Autowired
    private RiskControlService riskControlService;

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
