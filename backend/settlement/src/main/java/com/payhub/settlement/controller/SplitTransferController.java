package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.context.CurrentUserContext;
import com.payhub.common.result.Result;
import com.payhub.settlement.entity.PaySplitDetail;
import com.payhub.settlement.service.SplitEngineService;
import com.payhub.settlement.service.SplitTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/split-transfer")
public class SplitTransferController {

    @Autowired
    private SplitTransferService splitTransferService;

    @Autowired
    private SplitEngineService splitEngineService;

    @GetMapping("/pending")
    public Result<IPage<PaySplitDetail>> listPending(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer transferStatus) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        Map<String, Object> params = new HashMap<>();
        params.put("merchantNo", merchantNo);
        params.put("orderNo", orderNo);
        params.put("transferStatus", transferStatus);
        IPage<PaySplitDetail> page = splitTransferService.listPendingTransfers(current, size, params);
        return Result.success(page);
    }

    @PostMapping("/retry/{splitDetailId}")
    public Result<Void> retryTransfer(@PathVariable Long splitDetailId) {
        splitTransferService.retryTransfer(splitDetailId);
        return Result.success();
    }

    @PostMapping("/execute/{orderNo}")
    public Result<Void> executeTransferByOrder(@PathVariable String orderNo) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        var details = splitEngineService.getSplitDetailsByOrderNo(orderNo);
        if (details == null || details.isEmpty()) {
            return Result.success();
        }
        splitTransferService.executeTransferBatch(details);
        return Result.success();
    }

    @PostMapping("/process-pending")
    public Result<Map<String, Object>> processPending(@RequestParam(defaultValue = "100") int limit) {
        boolean success = splitTransferService.processPendingTransfers(limit);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("limit", limit);
        return Result.success(result);
    }
}
