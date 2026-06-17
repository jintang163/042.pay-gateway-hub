package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.settlement.dto.SplitRuleSaveRequest;
import com.payhub.settlement.dto.SplitRuleVO;
import com.payhub.settlement.service.SplitRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settlement/splitRule")
public class SplitRuleController {

    @Autowired
    private SplitRuleService splitRuleService;

    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody SplitRuleSaveRequest request) {
        splitRuleService.saveRule(request);
        return Result.success();
    }

    @PostMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        splitRuleService.deleteRule(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<SplitRuleVO> getById(@PathVariable Long id) {
        SplitRuleVO vo = splitRuleService.getRuleById(id);
        return Result.success(vo);
    }

    @GetMapping("/listByMerchant/{merchantNo}")
    public Result<List<SplitRuleVO>> listByMerchantNo(@PathVariable String merchantNo) {
        List<SplitRuleVO> list = splitRuleService.listByMerchantNo(merchantNo);
        return Result.success(list);
    }

    @GetMapping("/list")
    public Result<IPage<SplitRuleVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) String ruleName,
            @RequestParam(required = false) Integer status) {
        Map<String, Object> params = new HashMap<>();
        params.put("merchantNo", merchantNo);
        params.put("ruleName", ruleName);
        params.put("status", status);
        IPage<SplitRuleVO> page = splitRuleService.listPage(current, size, params);
        return Result.success(page);
    }
}
