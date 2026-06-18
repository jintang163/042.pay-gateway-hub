package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.settlement.dto.AgentProfitRecordVO;
import com.payhub.settlement.service.AgentProfitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent/profit")
public class AgentProfitController {

    @Autowired
    private AgentProfitService agentProfitService;

    @GetMapping("/{id}")
    public Result<AgentProfitRecordVO> getById(@PathVariable Long id) {
        AgentProfitRecordVO vo = agentProfitService.getProfitRecordById(id);
        return Result.success(vo);
    }

    @GetMapping("/list")
    public Result<IPage<AgentProfitRecordVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String profitNo,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) String agentMerchantNo,
            @RequestParam(required = false) Integer agentLevel,
            @RequestParam(required = false) Integer profitStatus,
            @RequestParam(required = false) String settleDate,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> params = new HashMap<>();
        params.put("profitNo", profitNo);
        params.put("orderNo", orderNo);
        params.put("merchantNo", merchantNo);
        params.put("agentMerchantNo", agentMerchantNo);
        params.put("agentLevel", agentLevel);
        params.put("profitStatus", profitStatus);
        params.put("settleDate", settleDate);
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        IPage<AgentProfitRecordVO> page = agentProfitService.listPage(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/list-by-agent/{agentMerchantNo}")
    public Result<List<AgentProfitRecordVO>> listByAgentMerchantNo(@PathVariable String agentMerchantNo) {
        List<AgentProfitRecordVO> list = agentProfitService.listByAgentMerchantNo(agentMerchantNo);
        return Result.success(list);
    }

    @GetMapping("/total/{agentMerchantNo}")
    public Result<BigDecimal> getTotalProfit(
            @PathVariable String agentMerchantNo,
            @RequestParam(required = false) Integer profitStatus) {
        BigDecimal total = agentProfitService.getTotalProfit(agentMerchantNo, profitStatus);
        return Result.success(total);
    }

    @PostMapping("/settle/{settleDate}")
    public Result<Void> settleAgentProfit(@PathVariable String settleDate) {
        agentProfitService.settleAgentProfit(settleDate);
        return Result.success();
    }
}
