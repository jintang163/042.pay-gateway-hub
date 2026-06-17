package com.payhub.risk.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.risk.dto.RiskListSaveRequest;
import com.payhub.risk.dto.RiskListVO;
import com.payhub.risk.entity.RiskBlacklist;
import com.payhub.risk.entity.RiskWhitelist;
import com.payhub.risk.service.RiskBlacklistService;
import com.payhub.risk.service.RiskWhitelistService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/risk/lists")
public class RiskListController {

    @Autowired
    private RiskBlacklistService riskBlacklistService;

    @Autowired
    private RiskWhitelistService riskWhitelistService;

    @GetMapping("/blacklist/page")
    public Result<IPage<RiskListVO>> listBlacklist(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String listType,
            @RequestParam(required = false) String listValue,
            @RequestParam(required = false) Integer status) {
        Map<String, Object> params = new HashMap<>();
        params.put("listType", listType);
        params.put("listValue", listValue);
        params.put("status", status);
        IPage<RiskListVO> page = riskBlacklistService.listPage(current, size, params);
        return Result.success(page);
    }

    @PostMapping("/blacklist")
    public Result<Void> addBlacklist(@Valid @RequestBody RiskListSaveRequest request) {
        RiskBlacklist blacklist = new RiskBlacklist();
        BeanUtils.copyProperties(request, blacklist);
        riskBlacklistService.save(blacklist);
        return Result.success();
    }

    @DeleteMapping("/blacklist/{id}")
    public Result<Void> deleteBlacklist(@PathVariable Long id) {
        riskBlacklistService.deleteById(id);
        return Result.success();
    }

    @GetMapping("/blacklist/check")
    public Result<Boolean> checkBlacklist(
            @RequestParam String listType,
            @RequestParam String listValue) {
        boolean inList = riskBlacklistService.checkInList(listType, listValue);
        return Result.success(inList);
    }

    @GetMapping("/whitelist/page")
    public Result<IPage<RiskListVO>> listWhitelist(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String listType,
            @RequestParam(required = false) String listValue,
            @RequestParam(required = false) Integer status) {
        Map<String, Object> params = new HashMap<>();
        params.put("listType", listType);
        params.put("listValue", listValue);
        params.put("status", status);
        IPage<RiskListVO> page = riskWhitelistService.listPage(current, size, params);
        return Result.success(page);
    }

    @PostMapping("/whitelist")
    public Result<Void> addWhitelist(@Valid @RequestBody RiskListSaveRequest request) {
        RiskWhitelist whitelist = new RiskWhitelist();
        BeanUtils.copyProperties(request, whitelist);
        riskWhitelistService.save(whitelist);
        return Result.success();
    }

    @DeleteMapping("/whitelist/{id}")
    public Result<Void> deleteWhitelist(@PathVariable Long id) {
        riskWhitelistService.deleteById(id);
        return Result.success();
    }

    @GetMapping("/whitelist/check")
    public Result<Boolean> checkWhitelist(
            @RequestParam String listType,
            @RequestParam String listValue,
            @RequestParam(required = false) String ruleCode) {
        boolean inList = riskWhitelistService.checkInList(listType, listValue, ruleCode);
        return Result.success(inList);
    }
}
