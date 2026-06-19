package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.settlement.dto.AutoWriteoffRuleSaveRequest;
import com.payhub.settlement.dto.AutoWriteoffRuleVO;
import com.payhub.settlement.dto.WriteoffRecordVO;
import com.payhub.settlement.service.ReconcileAutoWriteoffRuleService;
import com.payhub.settlement.service.ReconcileWriteoffRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settlement/writeoff")
public class ReconcileWriteoffController {

    @Autowired
    private ReconcileAutoWriteoffRuleService autoWriteoffRuleService;

    @Autowired
    private ReconcileWriteoffRecordService writeoffRecordService;

    @GetMapping("/rule/page")
    public Result<IPage<AutoWriteoffRuleVO>> listRules(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) String payChannel,
            @RequestParam(required = false) Integer diffType,
            @RequestParam(required = false) Integer enabled) {
        Map<String, Object> params = new HashMap<>();
        params.put("merchantNo", merchantNo);
        params.put("payChannel", payChannel);
        params.put("diffType", diffType);
        params.put("enabled", enabled);
        IPage<AutoWriteoffRuleVO> page = autoWriteoffRuleService.listPage(current, size, params);
        return Result.success(page);
    }

    @PostMapping("/rule")
    public Result<Void> addRule(@Valid @RequestBody AutoWriteoffRuleSaveRequest request) {
        autoWriteoffRuleService.addRule(request);
        return Result.success();
    }

    @PutMapping("/rule")
    public Result<Void> updateRule(@Valid @RequestBody AutoWriteoffRuleSaveRequest request) {
        autoWriteoffRuleService.updateRule(request);
        return Result.success();
    }

    @DeleteMapping("/rule/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        autoWriteoffRuleService.deleteRule(id);
        return Result.success();
    }

    @GetMapping("/record/page")
    public Result<IPage<WriteoffRecordVO>> listRecords(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String reconcileNo,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) String payChannel,
            @RequestParam(required = false) Integer writeoffStatus,
            @RequestParam(required = false) Integer writeoffSource,
            @RequestParam(required = false) Integer writeoffType) {
        Map<String, Object> params = new HashMap<>();
        params.put("reconcileNo", reconcileNo);
        params.put("merchantNo", merchantNo);
        params.put("payChannel", payChannel);
        params.put("writeoffStatus", writeoffStatus);
        params.put("writeoffSource", writeoffSource);
        params.put("writeoffType", writeoffType);
        IPage<WriteoffRecordVO> page = writeoffRecordService.listPage(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/record/{writeoffNo}")
    public Result<WriteoffRecordVO> getByWriteoffNo(@PathVariable String writeoffNo) {
        WriteoffRecordVO vo = writeoffRecordService.getByWriteoffNo(writeoffNo);
        return Result.success(vo);
    }

    @PostMapping("/record/{id}/execute")
    public Result<Void> executeWriteoff(@PathVariable Long id) {
        writeoffRecordService.executeWriteoff(id);
        return Result.success();
    }

    @PostMapping("/record/{id}/retry")
    public Result<Void> retryWriteoff(@PathVariable Long id) {
        writeoffRecordService.retryWriteoff(id);
        return Result.success();
    }
}
