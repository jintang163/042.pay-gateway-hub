package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.context.CurrentUserContext;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.Result;
import com.payhub.common.result.ResultCode;
import com.payhub.settlement.dto.AgentRelationSaveRequest;
import com.payhub.settlement.dto.AgentRelationVO;
import com.payhub.settlement.dto.AgentStatsVO;
import com.payhub.settlement.dto.AgentTreeVO;
import com.payhub.settlement.service.AgentRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent/relation")
public class AgentRelationController {

    @Autowired
    private AgentRelationService agentRelationService;

    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody AgentRelationSaveRequest request) {
        agentRelationService.saveAgentRelation(request);
        return Result.success();
    }

    @PostMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        agentRelationService.deleteAgentRelation(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<AgentRelationVO> getById(@PathVariable Long id) {
        AgentRelationVO vo = agentRelationService.getAgentRelationById(id);
        return Result.success(vo);
    }

    @GetMapping("/by-merchant/{merchantNo}")
    public Result<AgentRelationVO> getByMerchantNo(@PathVariable String merchantNo) {
        AgentRelationVO vo = agentRelationService.getAgentRelationByMerchantNo(merchantNo);
        return Result.success(vo);
    }

    @GetMapping("/list")
    public Result<IPage<AgentRelationVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) String merchantName,
            @RequestParam(required = false) String parentMerchantNo,
            @RequestParam(required = false) Integer agentLevel,
            @RequestParam(required = false) Integer status) {
        Map<String, Object> params = new HashMap<>();
        if (!CurrentUserContext.isAdmin()) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            params.put("parentMerchantNoPath", currentMerchantNo);
        } else {
            params.put("merchantNo", merchantNo);
            params.put("parentMerchantNo", parentMerchantNo);
        }
        params.put("merchantName", merchantName);
        params.put("agentLevel", agentLevel);
        params.put("status", status);
        IPage<AgentRelationVO> page = agentRelationService.listPage(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/tree/{merchantNo}")
    public Result<List<AgentTreeVO>> getAgentTree(@PathVariable String merchantNo) {
        if (!CurrentUserContext.isAdmin()) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(merchantNo)) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看其他商户的代理树");
            }
        }
        List<AgentTreeVO> tree = agentRelationService.getAgentTree(merchantNo);
        return Result.success(tree);
    }

    @GetMapping("/subordinates/direct/{parentMerchantNo}")
    public Result<List<AgentRelationVO>> listDirectSubordinates(@PathVariable String parentMerchantNo) {
        if (!CurrentUserContext.isAdmin()) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(parentMerchantNo)) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看其他商户的下级");
            }
        }
        List<AgentRelationVO> list = agentRelationService.listDirectSubordinates(parentMerchantNo);
        return Result.success(list);
    }

    @GetMapping("/subordinates/all/{merchantNo}")
    public Result<List<AgentRelationVO>> listAllSubordinates(@PathVariable String merchantNo) {
        if (!CurrentUserContext.isAdmin()) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(merchantNo)) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看其他商户的下级");
            }
        }
        List<AgentRelationVO> list = agentRelationService.listAllSubordinates(merchantNo);
        return Result.success(list);
    }

    @GetMapping("/stats/{merchantNo}")
    public Result<AgentStatsVO> getAgentStats(@PathVariable String merchantNo) {
        if (!CurrentUserContext.isAdmin()) {
            String currentMerchantNo = CurrentUserContext.getCurrentMerchantNo();
            if (!currentMerchantNo.equals(merchantNo)) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看其他商户的统计数据");
            }
        }
        AgentStatsVO stats = agentRelationService.getAgentStats(merchantNo);
        return Result.success(stats);
    }

    @PostMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        agentRelationService.updateAgentStatus(id, status);
        return Result.success();
    }
}
