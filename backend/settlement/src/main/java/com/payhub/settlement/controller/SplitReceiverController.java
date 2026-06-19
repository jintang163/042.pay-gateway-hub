package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.context.CurrentUserContext;
import com.payhub.common.result.Result;
import com.payhub.settlement.dto.SplitReceiverBatchImportItem;
import com.payhub.settlement.dto.SplitReceiverSaveRequest;
import com.payhub.settlement.dto.SplitReceiverVO;
import com.payhub.settlement.dto.SplitReceiverVerifyLogVO;
import com.payhub.settlement.dto.SplitReceiverVerifyRequest;
import com.payhub.settlement.service.SplitReceiverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/split-receiver")
public class SplitReceiverController {

    @Autowired
    private SplitReceiverService splitReceiverService;

    @GetMapping("/list")
    public Result<IPage<SplitReceiverVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String receiverNo,
            @RequestParam(required = false) String receiverName,
            @RequestParam(required = false) Integer receiverType,
            @RequestParam(required = false) Integer verifyStatus,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String idCardNo,
            @RequestParam(required = false) String bankCardNo) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        Map<String, Object> params = new HashMap<>();
        params.put("receiverNo", receiverNo);
        params.put("receiverName", receiverName);
        params.put("receiverType", receiverType);
        params.put("verifyStatus", verifyStatus);
        params.put("status", status);
        params.put("idCardNo", idCardNo);
        params.put("bankCardNo", bankCardNo);
        IPage<SplitReceiverVO> page = splitReceiverService.listPage(current, size, merchantNo, params);
        return Result.success(page);
    }

    @GetMapping("/{receiverNo}")
    public Result<SplitReceiverVO> getByReceiverNo(@PathVariable String receiverNo) {
        SplitReceiverVO vo = splitReceiverService.getByReceiverNo(receiverNo);
        return Result.success(vo);
    }

    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody SplitReceiverSaveRequest request) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        String operatorId = getCurrentOperatorId();
        String operatorName = getCurrentOperatorName();
        splitReceiverService.saveReceiver(request, merchantNo, operatorId, operatorName);
        return Result.success();
    }

    @PostMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable Long id) {
        splitReceiverService.toggleStatus(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        splitReceiverService.deleteReceiver(id);
        return Result.success();
    }

    @PostMapping("/verify")
    public Result<Void> verify(@Valid @RequestBody SplitReceiverVerifyRequest request) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        String operatorId = getCurrentOperatorId();
        String operatorName = getCurrentOperatorName();
        splitReceiverService.verifyReceiver(request, merchantNo, operatorId, operatorName);
        return Result.success();
    }

    @GetMapping("/available")
    public Result<List<SplitReceiverVO>> listAvailable() {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        List<SplitReceiverVO> list = splitReceiverService.listAvailableReceivers(merchantNo);
        return Result.success(list);
    }

    @PostMapping("/batch-import")
    public Result<Map<String, Object>> batchImport(@RequestBody List<SplitReceiverBatchImportItem> items) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        String operatorId = getCurrentOperatorId();
        String operatorName = getCurrentOperatorName();
        Map<String, Object> result = splitReceiverService.batchImport(items, merchantNo, operatorId, operatorName);
        return Result.success(result);
    }

    @GetMapping("/verify-logs")
    public Result<IPage<SplitReceiverVerifyLogVO>> listVerifyLogs(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String receiverNo,
            @RequestParam(required = false) Integer verifyChannel,
            @RequestParam(required = false) Integer verifyStatus,
            @RequestParam(required = false) String verifyRequestId) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        Map<String, Object> params = new HashMap<>();
        params.put("receiverNo", receiverNo);
        params.put("verifyChannel", verifyChannel);
        params.put("verifyStatus", verifyStatus);
        params.put("verifyRequestId", verifyRequestId);
        IPage<SplitReceiverVerifyLogVO> page = splitReceiverService.listVerifyLogs(current, size, merchantNo, params);
        return Result.success(page);
    }

    private String getCurrentOperatorId() {
        Object user = CurrentUserContext.getCurrentUser();
        if (user == null) {
            return null;
        }
        try {
            java.lang.reflect.Method method = user.getClass().getMethod("getId");
            Object id = method.invoke(user);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getCurrentOperatorName() {
        Object user = CurrentUserContext.getCurrentUser();
        if (user == null) {
            return null;
        }
        try {
            java.lang.reflect.Method method = user.getClass().getMethod("getUsername");
            Object name = method.invoke(user);
            return name != null ? name.toString() : null;
        } catch (Exception e) {
            try {
                java.lang.reflect.Method method = user.getClass().getMethod("getName");
                Object name = method.invoke(user);
                return name != null ? name.toString() : null;
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
