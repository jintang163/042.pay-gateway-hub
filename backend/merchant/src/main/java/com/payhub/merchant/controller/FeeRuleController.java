package com.payhub.merchant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.merchant.dto.*;
import com.payhub.merchant.service.FeeRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/fee-rule")
public class FeeRuleController {

    @Autowired
    private FeeRuleService feeRuleService;

    @GetMapping("/list")
    public Result<IPage<FeeRuleVO>> list(@RequestParam(defaultValue = "1") int current,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String industryCode,
                                         @RequestParam(required = false) String payChannel,
                                         @RequestParam(required = false) Integer status) {
        return Result.success(feeRuleService.listPage(current, size, industryCode, payChannel, status));
    }

    @GetMapping("/list-by-industry")
    public Result<List<FeeRuleVO>> listByIndustry(@RequestParam String industryCode,
                                                  @RequestParam(required = false) String payChannel) {
        return Result.success(feeRuleService.listByIndustry(industryCode, payChannel));
    }

    @GetMapping("/industries")
    public Result<List<Map<String, String>>> listIndustries() {
        return Result.success(feeRuleService.listIndustries());
    }

    @GetMapping("/{ruleNo}")
    public Result<List<FeeRuleVO>> getByRuleNo(@PathVariable String ruleNo) {
        return Result.success(feeRuleService.getByRuleNo(ruleNo));
    }

    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody FeeRuleSaveRequest request) {
        feeRuleService.saveRule(request);
        return Result.success();
    }

    @PostMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable Long id) {
        feeRuleService.toggleStatus(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        feeRuleService.deleteRule(id);
        return Result.success();
    }

    @PostMapping("/calculate")
    public Result<FeeCalcResult> calculate(@Valid @RequestBody FeeCalcRequest request) {
        return Result.success(feeRuleService.calculate(request));
    }
}
