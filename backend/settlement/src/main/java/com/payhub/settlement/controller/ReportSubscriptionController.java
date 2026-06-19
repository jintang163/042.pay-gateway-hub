package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.context.CurrentUserContext;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.Result;
import com.payhub.common.result.ResultCode;
import com.payhub.settlement.dto.ReportSubscriptionSaveRequest;
import com.payhub.settlement.dto.ReportSubscriptionVO;
import com.payhub.settlement.service.ReportPushService;
import com.payhub.settlement.service.ReportSubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/report/subscription")
public class ReportSubscriptionController {

    @Autowired
    private ReportSubscriptionService reportSubscriptionService;

    @Autowired
    private ReportPushService reportPushService;

    @GetMapping("/page")
    public Result<IPage<ReportSubscriptionVO>> listPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam Map<String, Object> params) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        if (params == null) {
            params = new HashMap<>();
        }
        params.put("merchantNo", merchantNo);
        IPage<ReportSubscriptionVO> page = reportSubscriptionService.listPage(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result<ReportSubscriptionVO> getById(@PathVariable Long id) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        if (!reportSubscriptionService.validateOwnership(id, merchantNo)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看该报表订阅");
        }
        ReportSubscriptionVO vo = reportSubscriptionService.getSubscriptionById(id);
        return Result.success(vo);
    }

    @PostMapping("/")
    public Result<Void> save(@RequestBody @Valid ReportSubscriptionSaveRequest request) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        request.setMerchantNo(merchantNo);
        reportSubscriptionService.saveSubscription(request);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid ReportSubscriptionSaveRequest request) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        if (!reportSubscriptionService.validateOwnership(id, merchantNo)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限修改该报表订阅");
        }
        request.setMerchantNo(merchantNo);
        reportSubscriptionService.updateSubscription(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        if (!reportSubscriptionService.validateOwnership(id, merchantNo)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限删除该报表订阅");
        }
        reportSubscriptionService.deleteSubscription(id);
        return Result.success();
    }

    @PostMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable Long id) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        if (!reportSubscriptionService.validateOwnership(id, merchantNo)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限操作该报表订阅");
        }
        reportSubscriptionService.toggleSubscription(id);
        return Result.success();
    }

    @PostMapping("/{id}/push")
    public Result<Void> manualPush(@PathVariable Long id) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        if (!reportSubscriptionService.validateOwnership(id, merchantNo)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限推送该报表订阅");
        }
        reportPushService.manualPush(id);
        return Result.success();
    }
}
