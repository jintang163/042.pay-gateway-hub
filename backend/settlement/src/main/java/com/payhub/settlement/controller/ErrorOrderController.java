package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.settlement.dto.ErrorOrderApplyRequest;
import com.payhub.settlement.dto.ErrorOrderAuditRequest;
import com.payhub.settlement.dto.ErrorOrderVO;
import com.payhub.settlement.service.ErrorOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settlement/error-order")
public class ErrorOrderController {

    @Autowired
    private ErrorOrderService errorOrderService;

    @GetMapping("/list")
    public Result<IPage<ErrorOrderVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String errorNo,
            @RequestParam(required = false) String reconcileNo,
            @RequestParam(required = false) String payChannel,
            @RequestParam(required = false) Integer errorType,
            @RequestParam(required = false) Integer errorStatus,
            @RequestParam(required = false) Integer auditStatus,
            @RequestParam(required = false) Integer handleType,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String merchantNo) {
        Map<String, Object> params = new HashMap<>();
        params.put("errorNo", errorNo);
        params.put("reconcileNo", reconcileNo);
        params.put("payChannel", payChannel);
        params.put("errorType", errorType);
        params.put("errorStatus", errorStatus);
        params.put("auditStatus", auditStatus);
        params.put("handleType", handleType);
        params.put("orderNo", orderNo);
        params.put("merchantNo", merchantNo);
        IPage<ErrorOrderVO> page = errorOrderService.listPage(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/{errorNo}")
    public Result<ErrorOrderVO> getByErrorNo(@PathVariable String errorNo) {
        ErrorOrderVO vo = errorOrderService.getByErrorNo(errorNo);
        return Result.success(vo);
    }

    @GetMapping("/id/{id}")
    public Result<ErrorOrderVO> getById(@PathVariable Long id) {
        ErrorOrderVO vo = errorOrderService.getById(id);
        return Result.success(vo);
    }

    @GetMapping("/list-by-detail/{detailId}")
    public Result<List<ErrorOrderVO>> listByReconcileDetailId(@PathVariable Long detailId) {
        List<ErrorOrderVO> list = errorOrderService.listByReconcileDetailId(detailId);
        return Result.success(list);
    }

    @PostMapping("/apply")
    public Result<ErrorOrderVO> applyErrorOrder(
            @RequestBody ErrorOrderApplyRequest request,
            @RequestParam(required = false) String applyUserId,
            @RequestParam(required = false) String applyUserName) {
        ErrorOrderVO vo = errorOrderService.applyErrorOrder(request, applyUserId, applyUserName);
        return Result.success(vo);
    }

    @PostMapping("/audit")
    public Result<ErrorOrderVO> auditErrorOrder(
            @RequestBody ErrorOrderAuditRequest request,
            @RequestParam(required = false) String auditUserId,
            @RequestParam(required = false) String auditUserName) {
        ErrorOrderVO vo = errorOrderService.auditErrorOrder(request, auditUserId, auditUserName);
        return Result.success(vo);
    }

    @PostMapping("/process-supplement/{id}")
    public Result<ErrorOrderVO> processSupplementOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String handleUserId,
            @RequestParam(required = false) String handleUserName) {
        ErrorOrderVO vo = errorOrderService.processSupplementOrder(id, handleUserId, handleUserName);
        return Result.success(vo);
    }

    @PostMapping("/process-refund/{id}")
    public Result<ErrorOrderVO> processRefund(
            @PathVariable Long id,
            @RequestParam(required = false) String handleUserId,
            @RequestParam(required = false) String handleUserName) {
        ErrorOrderVO vo = errorOrderService.processRefund(id, handleUserId, handleUserName);
        return Result.success(vo);
    }

    @PostMapping("/process-adjust/{id}")
    public Result<ErrorOrderVO> processAdjust(
            @PathVariable Long id,
            @RequestParam(required = false) String handleUserId,
            @RequestParam(required = false) String handleUserName) {
        ErrorOrderVO vo = errorOrderService.processAdjust(id, handleUserId, handleUserName);
        return Result.success(vo);
    }

    @PostMapping("/process-ignore/{id}")
    public Result<ErrorOrderVO> processIgnore(
            @PathVariable Long id,
            @RequestParam(required = false) String handleUserId,
            @RequestParam(required = false) String handleUserName) {
        ErrorOrderVO vo = errorOrderService.processIgnore(id, handleUserId, handleUserName);
        return Result.success(vo);
    }
}
