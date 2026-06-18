package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.settlement.dto.ReconcileSummaryVO;
import com.payhub.settlement.dto.ReconcileVO;
import com.payhub.settlement.service.ReconcileDetailService;
import com.payhub.settlement.service.ReconcileService;
import javax.servlet.http.HttpServletResponse;
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

    @Autowired
    private ReconcileDetailService reconcileDetailService;

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

    @GetMapping("/summary")
    public Result<ReconcileSummaryVO> getSummary(
            @RequestParam(required = false) String reconcileNo,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate reconcileDate,
            @RequestParam(required = false) String payChannel) {
        ReconcileSummaryVO summary;
        if (reconcileNo != null && !reconcileNo.isEmpty()) {
            summary = reconcileDetailService.getSummary(reconcileNo);
        } else if (reconcileDate != null && payChannel != null) {
            summary = reconcileDetailService.getSummaryByDateAndChannel(reconcileDate, payChannel);
            ReconcileVO record = reconcileService.getByReconcileNo(
                    findReconcileNoByDateAndChannel(reconcileDate, payChannel)
            );
            if (record != null) {
                summary.setReconcileNo(record.getReconcileNo());
                summary.setTotalCount(record.getTotalCount());
                summary.setMatchCount(record.getMatchCount());
                summary.setMismatchCount(record.getMismatchCount());
            }
        } else {
            summary = new ReconcileSummaryVO();
        }
        return Result.success(summary);
    }

    @GetMapping("/export")
    public void exportDetails(
            @RequestParam(required = false) String reconcileNo,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate reconcileDate,
            @RequestParam(required = false) String payChannel,
            @RequestParam(required = false) Integer diffType,
            @RequestParam(required = false) Integer handleStatus,
            HttpServletResponse response) {
        Map<String, Object> params = new HashMap<>();
        params.put("reconcileNo", reconcileNo);
        params.put("reconcileDate", reconcileDate);
        params.put("payChannel", payChannel);
        params.put("diffType", diffType);
        params.put("handleStatus", handleStatus);
        reconcileDetailService.exportDetailsByCondition(params, response);
    }

    private String findReconcileNoByDateAndChannel(LocalDate reconcileDate, String payChannel) {
        Map<String, Object> params = new HashMap<>();
        params.put("reconcileDate", reconcileDate);
        params.put("payChannel", payChannel);
        IPage<ReconcileVO> page = reconcileService.listPage(1L, 1L, params);
        if (page != null && page.getRecords() != null && !page.getRecords().isEmpty()) {
            return page.getRecords().get(0).getReconcileNo();
        }
        return null;
    }
}
