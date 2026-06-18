package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.context.CurrentUserContext;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.Result;
import com.payhub.common.result.ResultCode;
import com.payhub.settlement.dto.AgentProfitRuleSaveRequest;
import com.payhub.settlement.dto.AgentProfitRuleVO;
import com.payhub.settlement.service.AgentProfitRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent/profit-rule")
public class AgentProfitRuleController {

    @Autowired
    private AgentProfitRuleService agentProfitRuleService;

    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody AgentProfitRuleSaveRequest request) {
        if (!CurrentUserContext.isAdmin()) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(request.getMerchantNo())) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限为其他商户设置分润规则");
            }
        }
        agentProfitRuleService.saveRule(request);
        return Result.success();
    }

    @PostMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (!CurrentUserContext.isAdmin()) {
            AgentProfitRuleVO vo = agentProfitRuleService.getRuleById(id);
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (vo != null && !currentMerchantNo.equals(vo.getMerchantNo())) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限删除其他商户的分润规则");
            }
        }
        agentProfitRuleService.deleteRule(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<AgentProfitRuleVO> getById(@PathVariable Long id) {
        AgentProfitRuleVO vo = agentProfitRuleService.getRuleById(id);
        if (!CurrentUserContext.isAdmin() && vo != null) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(vo.getMerchantNo())) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看该分润规则");
            }
        }
        return Result.success(vo);
    }

    @GetMapping("/by-rule-no/{ruleNo}")
    public Result<AgentProfitRuleVO> getByRuleNo(@PathVariable String ruleNo) {
        AgentProfitRuleVO vo = agentProfitRuleService.getRuleByRuleNo(ruleNo);
        if (!CurrentUserContext.isAdmin() && vo != null) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(vo.getMerchantNo())) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看该分润规则");
            }
        }
        return Result.success(vo);
    }

    @GetMapping("/listByMerchant/{merchantNo}")
    public Result<List<AgentProfitRuleVO>> listByMerchantNo(@PathVariable String merchantNo) {
        if (!CurrentUserContext.isAdmin()) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(merchantNo)) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看其他商户的分润规则");
            }
        }
        List<AgentProfitRuleVO> list = agentProfitRuleService.listByMerchantNo(merchantNo);
        return Result.success(list);
    }

    @GetMapping("/list")
    public Result<IPage<AgentProfitRuleVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String ruleNo,
            @RequestParam(required = false) String ruleName,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) Integer agentLevel,
            @RequestParam(required = false) Integer status) {
        Map<String, Object> params = new HashMap<>();
        params.put("ruleNo", ruleNo);
        params.put("ruleName", ruleName);
        if (!CurrentUserContext.isAdmin()) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            params.put("merchantNo", currentMerchantNo);
        } else {
            params.put("merchantNo", merchantNo);
        }
        params.put("agentLevel", agentLevel);
        params.put("status", status);
        IPage<AgentProfitRuleVO> page = agentProfitRuleService.listPage(current, size, params);
        return Result.success(page);
    }

    @PostMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable Long id) {
        if (!CurrentUserContext.isAdmin()) {
            AgentProfitRuleVO vo = agentProfitRuleService.getRuleById(id);
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (vo != null && !currentMerchantNo.equals(vo.getMerchantNo())) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限修改其他商户的分润规则");
            }
        }
        agentProfitRuleService.toggleRule(id);
        return Result.success();
    }
}
