package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.settlement.dto.AgentProfitRuleSaveRequest;
import com.payhub.settlement.dto.AgentProfitRuleVO;
import com.payhub.settlement.service.AgentProfitRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent/profit-rule")
public class AgentProfitRuleController {

    @Autowired
    private AgentProfitRuleService agentProfitRuleService;

    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody AgentProfitRuleSaveRequest request) {
        agentProfitRuleService.saveRule(request);
        return Result.success();
    }

    @PostMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        agentProfitRuleService.deleteRule(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<AgentProfitRuleVO> getById(@PathVariable Long id) {
        AgentProfitRuleVO vo = agentProfitRuleService.getRuleById(id);
        return Result.success(vo);
    }

    @GetMapping("/by-rule-no/{ruleNo}")
    public Result<AgentProfitRuleVO> getByRuleNo(@PathVariable String ruleNo) {
        AgentProfitRuleVO vo = agentProfitRuleService.getRuleByRuleNo(ruleNo);
        return Result.success(vo);
    }

    @GetMapping("/listByMerchant/{merchantNo}")
    public Result<List<AgentProfitRuleVO>> listByMerchantNo(@PathVariable String merchantNo) {
        List<AgentProfitRuleVO> list = agentProfitRuleService.listByMerchantNo(merchantNo);
        return Result.success(list);
    }

    @GetMapping("/list")
    public Result<IPage<AgentProfitRuleVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String ruleNo,
            @RequestParam(required = false) String ruleName,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) Integer agentLevel,
            @RequestParam(required = false) Integer status) {
        Map<String, Object> params = new HashMap<>();
        params.put("ruleNo", ruleNo);
        params.put("ruleName", ruleName);
        params.put("merchantNo", merchantNo);
        params.put("agentLevel", agentLevel);
        params.put("status", status);
        IPage<AgentProfitRuleVO> page = agentProfitRuleService.listPage(current, size, params);
        return Result.success(page);
    }

    @PostMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable Long id) {
        agentProfitRuleService.toggleRule(id);
        return Result.success();
    }
}
