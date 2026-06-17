package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.settlement.dto.SettlementVO;
import com.payhub.settlement.entity.PaySplitDetail;
import com.payhub.settlement.service.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settlement")
public class SettlementController {

    @Autowired
    private SettlementService settlementService;

    @GetMapping("/list")
    public Result<IPage<SettlementVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate settleDate,
            @RequestParam(required = false) Integer settleStatus,
            @RequestParam(required = false) String payChannel) {
        Map<String, Object> params = new HashMap<>();
        params.put("merchantNo", merchantNo);
        params.put("settleDate", settleDate);
        params.put("settleStatus", settleStatus);
        params.put("payChannel", payChannel);
        IPage<SettlementVO> page = settlementService.listPage(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result<SettlementVO> getDetail(@PathVariable Long id) {
        SettlementVO vo = settlementService.getSettlementDetail(id);
        return Result.success(vo);
    }

    @PostMapping("/generate")
    public Result<Void> generateSettlement(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate settleDate) {
        settlementService.generateSettlement(settleDate);
        return Result.success();
    }

    @PostMapping("/{id}/confirm")
    public Result<Void> confirmSettle(@PathVariable Long id) {
        settlementService.confirmSettle(id);
        return Result.success();
    }

    @PostMapping("/{id}/retry")
    public Result<Void> retrySettlement(@PathVariable Long id) {
        settlementService.retrySettlement(id);
        return Result.success();
    }

    @PostMapping("/retry-all")
    public Result<Void> retryAllFailed() {
        settlementService.retryFailedSettlement();
        return Result.success();
    }

    @GetMapping("/{id}/details")
    public Result<IPage<PaySplitDetail>> getSettlementDetails(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        IPage<PaySplitDetail> page = settlementService.listSettlementDetails(current, size, id);
        return Result.success(page);
    }

    @PostMapping("/execute-task")
    public Result<Void> executeTask() {
        settlementService.executeSettlementTask();
        return Result.success();
    }
}
