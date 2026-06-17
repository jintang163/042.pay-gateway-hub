package com.payhub.risk.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.risk.dto.RiskRuleSaveRequest;
import com.payhub.risk.entity.RiskRule;
import com.payhub.risk.service.RiskRuleService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/risk/rules")
public class RiskRuleController {

    @Autowired
    private RiskRuleService riskRuleService;

    @GetMapping("/page")
    public Result<IPage<RiskRule>> listPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) Integer riskLevel,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String ruleName) {
        Map<String, Object> params = new HashMap<>();
        params.put("ruleType", ruleType);
        params.put("riskLevel", riskLevel);
        params.put("status", status);
        params.put("ruleName", ruleName);
        IPage<RiskRule> page = riskRuleService.listPage(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result<RiskRule> getById(@PathVariable Long id) {
        RiskRule rule = riskRuleService.getById(id);
        return Result.success(rule);
    }

    @PostMapping("/")
    public Result<Void> save(@Valid @RequestBody RiskRuleSaveRequest request) {
        RiskRule rule = new RiskRule();
        BeanUtils.copyProperties(request, rule);
        riskRuleService.save(rule);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody RiskRuleSaveRequest request) {
        RiskRule rule = new RiskRule();
        BeanUtils.copyProperties(request, rule);
        rule.setId(id);
        riskRuleService.update(rule);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        riskRuleService.deleteById(id);
        return Result.success();
    }

    @PostMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        riskRuleService.enableRule(id);
        return Result.success();
    }

    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        riskRuleService.disableRule(id);
        return Result.success();
    }

    @PostMapping("/reload")
    public Result<Void> reload() {
        riskRuleService.reloadAllRules();
        return Result.success();
    }

    @GetMapping("/{id}/preview")
    public Result<String> preview(@PathVariable Long id) {
        String content = riskRuleService.generateRuleContent(id);
        return Result.success(content);
    }
}
