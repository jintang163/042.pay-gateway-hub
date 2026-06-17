package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.settlement.dto.ReconcileVO;
import com.payhub.settlement.service.ReconcileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settlement/reconcile")
public class ReconcileController {

    @Autowired
    private ReconcileService reconcileService;

    @GetMapping("/{reconcileNo}")
    public Result<ReconcileVO> getByReconcileNo(@PathVariable String reconcileNo) {
        ReconcileVO vo = reconcileService.getByReconcileNo(reconcileNo);
        return Result.success(vo);
    }

    @GetMapping("/list")
    public Result<IPage<ReconcileVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String payChannel,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate reconcileDate,
            @RequestParam(required = false) Integer reconcileStatus) {
        Map<String, Object> params = new HashMap<>();
        params.put("payChannel", payChannel);
        params.put("reconcileDate", reconcileDate);
        params.put("reconcileStatus", reconcileStatus);
        IPage<ReconcileVO> page = reconcileService.listPage(current, size, params);
        return Result.success(page);
    }

    @PostMapping("/execute")
    public Result<Void> executeReconcile(
            @RequestParam String payChannel,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate reconcileDate) {
        reconcileService.executeReconcile(payChannel, reconcileDate);
        return Result.success();
    }
}
