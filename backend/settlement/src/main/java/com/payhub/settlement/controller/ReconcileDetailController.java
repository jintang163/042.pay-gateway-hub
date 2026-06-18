package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.settlement.dto.ReconcileDetailVO;
import com.payhub.settlement.service.ReconcileDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settlement/reconcile-detail")
public class ReconcileDetailController {

    @Autowired
    private ReconcileDetailService reconcileDetailService;

    @GetMapping("/list")
    public Result<IPage<ReconcileDetailVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String reconcileNo,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate reconcileDate,
            @RequestParam(required = false) String payChannel,
            @RequestParam(required = false) Integer diffType,
            @RequestParam(required = false) Integer handleStatus,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) String channelTradeNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("reconcileNo", reconcileNo);
        params.put("reconcileDate", reconcileDate);
        params.put("payChannel", payChannel);
        params.put("diffType", diffType);
        params.put("handleStatus", handleStatus);
        params.put("orderNo", orderNo);
        params.put("merchantNo", merchantNo);
        params.put("channelTradeNo", channelTradeNo);
        IPage<ReconcileDetailVO> page = reconcileDetailService.listPage(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/list-by-reconcile/{reconcileNo}")
    public Result<List<ReconcileDetailVO>> listByReconcileNo(@PathVariable String reconcileNo) {
        List<ReconcileDetailVO> list = reconcileDetailService.listByReconcileNo(reconcileNo);
        return Result.success(list);
    }

    @PostMapping("/handle")
    public Result<Void> handleDetail(
            @RequestParam Long detailId,
            @RequestParam Integer handleStatus,
            @RequestParam(required = false) String handleRemark,
            @RequestParam(required = false) String handleUserId,
            @RequestParam(required = false) String handleUserName) {
        reconcileDetailService.handleDetail(detailId, handleStatus, handleRemark, handleUserId, handleUserName);
        return Result.success();
    }

    @PostMapping("/ignore/{detailId}")
    public Result<Void> ignoreDetail(
            @PathVariable Long detailId,
            @RequestParam(required = false) String handleRemark,
            @RequestParam(required = false) String handleUserId,
            @RequestParam(required = false) String handleUserName) {
        reconcileDetailService.handleDetail(detailId, 3, handleRemark, handleUserId, handleUserName);
        return Result.success();
    }
}
