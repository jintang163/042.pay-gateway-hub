package com.payhub.risk.controller;

import com.payhub.common.result.Result;
import com.payhub.risk.dto.ApiStatsVO;
import com.payhub.risk.service.ApiAccessLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/api-stats")
public class ApiStatsController {

    @Autowired
    private ApiAccessLogService apiAccessLogService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverviewStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Map<String, Object> stats = apiAccessLogService.getOverviewStats(startTime, endTime);
        return Result.success(stats);
    }

    @GetMapping("/list")
    public Result<List<ApiStatsVO>> getApiStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        List<ApiStatsVO> stats = apiAccessLogService.getApiStats(startTime, endTime);
        return Result.success(stats);
    }

    @GetMapping("/top-merchants")
    public Result<List<Map<String, Object>>> getTopMerchants(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<Map<String, Object>> topMerchants = apiAccessLogService.getTopMerchants(startTime, endTime, limit);
        return Result.success(topMerchants);
    }
}
