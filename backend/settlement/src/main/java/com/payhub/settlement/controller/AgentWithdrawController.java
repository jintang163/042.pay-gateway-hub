package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.context.CurrentUserContext;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.Result;
import com.payhub.common.result.ResultCode;
import com.payhub.settlement.dto.AgentWithdrawApplyRequest;
import com.payhub.settlement.dto.AgentWithdrawAuditRequest;
import com.payhub.settlement.dto.AgentWithdrawVO;
import com.payhub.settlement.service.AgentWithdrawService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent/withdraw")
public class AgentWithdrawController {

    @Autowired
    private AgentWithdrawService agentWithdrawService;

    @PostMapping("/apply")
    public Result<Void> apply(@Valid @RequestBody AgentWithdrawApplyRequest request) {
        if (!CurrentUserContext.isAdmin()) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(request.getMerchantNo())) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限为其他商户申请提现");
            }
        }
        agentWithdrawService.applyWithdraw(request);
        return Result.success();
    }

    @PostMapping("/audit")
    public Result<Void> audit(@Valid @RequestBody AgentWithdrawAuditRequest request) {
        if (!CurrentUserContext.isAdmin()) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "仅管理员可审核提现申请");
        }
        agentWithdrawService.auditWithdraw(request);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<AgentWithdrawVO> getById(@PathVariable Long id) {
        AgentWithdrawVO vo = agentWithdrawService.getWithdrawById(id);
        if (!CurrentUserContext.isAdmin() && vo != null) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(vo.getMerchantNo())) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看该提现记录");
            }
        }
        return Result.success(vo);
    }

    @GetMapping("/by-withdraw-no/{withdrawNo}")
    public Result<AgentWithdrawVO> getByWithdrawNo(@PathVariable String withdrawNo) {
        AgentWithdrawVO vo = agentWithdrawService.getWithdrawByWithdrawNo(withdrawNo);
        if (!CurrentUserContext.isAdmin() && vo != null) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(vo.getMerchantNo())) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看该提现记录");
            }
        }
        return Result.success(vo);
    }

    @GetMapping("/list")
    public Result<IPage<AgentWithdrawVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String withdrawNo,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) String merchantName,
            @RequestParam(required = false) Integer withdrawStatus,
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
        params.put("startDate", startDate);
        params.put("endDate", endDate);
        IPage<AgentWithdrawVO> page = agentWithdrawService.listPage(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/list-by-merchant/{merchantNo}")
    public Result<List<AgentWithdrawVO>> listByMerchantNo(@PathVariable String merchantNo) {
        if (!CurrentUserContext.isAdmin()) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(merchantNo)) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看其他商户的提现记录");
            }
        }
        List<AgentWithdrawVO> list = agentWithdrawService.listByMerchantNo(merchantNo);
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
        BigDecimal total = agentWithdrawService.getTotalWithdraw(merchantNo, withdrawStatus);
        return Result.success(total);
    }

    @GetMapping("/balance/{merchantNo}")
    public Result<BigDecimal> getAvailableBalance(@PathVariable String merchantNo) {
        if (!CurrentUserContext.isAdmin()) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(merchantNo)) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看其他商户的余额");
            }
        }
        BigDecimal balance = agentWithdrawService.getAvailableBalance(merchantNo);
        return Result.success(balance);
    }

    @PostMapping("/execute/{id}")
    public Result<Void> executeTransfer(@PathVariable Long id) {
        if (!CurrentUserContext.isAdmin()) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "仅管理员可执行转账操作");
        }
        agentWithdrawService.executeTransfer(id);
        return Result.success();
    }

    @PostMapping("/retry")
    public Result<Void> retryFailedWithdraw() {
        if (!CurrentUserContext.isAdmin()) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "仅管理员可执行重试操作");
        }
        agentWithdrawService.retryFailedWithdraw();
        return Result.success();
    }
}
