package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.context.CurrentUserContext;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.Result;
import com.payhub.common.result.ResultCode;
import com.payhub.settlement.dto.MerchantBalanceVO;
import com.payhub.settlement.dto.MerchantWithdrawApplyRequest;
import com.payhub.settlement.dto.MerchantWithdrawAuditRequest;
import com.payhub.settlement.dto.MerchantWithdrawVO;
import com.payhub.settlement.service.MerchantWithdrawService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant/withdraw")
public class MerchantWithdrawController {

    @Autowired
    private MerchantWithdrawService merchantWithdrawService;

    @PostMapping("/apply")
    public Result<Void> apply(@Valid @RequestBody MerchantWithdrawApplyRequest request) {
        if (!CurrentUserContext.isAdmin()) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(request.getMerchantNo())) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限为其他商户申请提现");
            }
        }
        merchantWithdrawService.applyWithdraw(request);
        return Result.success();
    }

    @PostMapping("/audit")
    public Result<Void> audit(@Valid @RequestBody MerchantWithdrawAuditRequest request) {
        if (!CurrentUserContext.isAdmin()) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "仅管理员可审核提现申请");
        }
        merchantWithdrawService.auditWithdraw(request);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<MerchantWithdrawVO> getById(@PathVariable Long id) {
        MerchantWithdrawVO vo = merchantWithdrawService.getWithdrawById(id);
        if (!CurrentUserContext.isAdmin() && vo != null) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(vo.getMerchantNo())) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看该提现记录");
            }
        }
        return Result.success(vo);
    }

    @GetMapping("/by-withdraw-no/{withdrawNo}")
    public Result<MerchantWithdrawVO> getByWithdrawNo(@PathVariable String withdrawNo) {
        MerchantWithdrawVO vo = merchantWithdrawService.getWithdrawByWithdrawNo(withdrawNo);
        if (!CurrentUserContext.isAdmin() && vo != null) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(vo.getMerchantNo())) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看该提现记录");
            }
        }
        return Result.success(vo);
    }

    @GetMapping("/list")
    public Result<IPage<MerchantWithdrawVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String withdrawNo,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) String merchantName,
            @RequestParam(required = false) Integer withdrawStatus,
            @RequestParam(required = false) Integer withdrawType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> params = new HashMap<>();
        params.put("withdrawNo", withdrawNo);
        if (!CurrentUserContext.isAdmin()) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            params.put("merchantNo", currentMerchantNo);
        } else {
            params.put("merchantNo", merchantNo);
        }
        params.put("merchantName", merchantName);
        params.put("withdrawStatus", withdrawStatus);
        params.put("withdrawType", withdrawType);
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        IPage<MerchantWithdrawVO> page = merchantWithdrawService.listPage(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/list-by-merchant/{merchantNo}")
    public Result<List<MerchantWithdrawVO>> listByMerchantNo(@PathVariable String merchantNo) {
        if (!CurrentUserContext.isAdmin()) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(merchantNo)) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看其他商户的提现记录");
            }
        }
        List<MerchantWithdrawVO> list = merchantWithdrawService.listByMerchantNo(merchantNo);
        return Result.success(list);
    }

    @GetMapping("/total/{merchantNo}")
    public Result<BigDecimal> getTotalWithdraw(
            @PathVariable String merchantNo,
            @RequestParam(required = false) Integer withdrawStatus) {
        if (!CurrentUserContext.isAdmin()) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(merchantNo)) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看其他商户的提现统计");
            }
        }
        BigDecimal total = merchantWithdrawService.getTotalWithdraw(merchantNo, withdrawStatus);
        return Result.success(total);
    }

    @GetMapping("/balance/{merchantNo}")
    public Result<MerchantBalanceVO> getMerchantBalance(@PathVariable String merchantNo) {
        if (!CurrentUserContext.isAdmin()) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(merchantNo)) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看其他商户的余额");
            }
        }
        MerchantBalanceVO balance = merchantWithdrawService.getMerchantBalance(merchantNo);
        return Result.success(balance);
    }

    @GetMapping("/available-balance/{merchantNo}")
    public Result<BigDecimal> getAvailableBalance(@PathVariable String merchantNo) {
        if (!CurrentUserContext.isAdmin()) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(merchantNo)) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看其他商户的余额");
            }
        }
        BigDecimal balance = merchantWithdrawService.getAvailableBalance(merchantNo);
        return Result.success(balance);
    }

    @GetMapping("/calculate-fee")
    public Result<BigDecimal> calculateFee(
            @RequestParam BigDecimal amount,
            @RequestParam Integer withdrawType) {
        BigDecimal fee = merchantWithdrawService.calculateFee(amount, withdrawType);
        return Result.success(fee);
    }

    @PostMapping("/execute/{id}")
    public Result<Void> executeTransfer(@PathVariable Long id) {
        if (!CurrentUserContext.isAdmin()) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "仅管理员可执行转账操作");
        }
        merchantWithdrawService.executeTransfer(id);
        return Result.success();
    }

    @PostMapping("/retry")
    public Result<Void> retryFailedWithdraw() {
        if (!CurrentUserContext.isAdmin()) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "仅管理员可执行重试操作");
        }
        merchantWithdrawService.retryFailedWithdraw();
        return Result.success();
    }
}
