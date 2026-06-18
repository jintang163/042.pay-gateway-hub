package com.payhub.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.settlement.dto.AgentProfitRuleSaveRequest;
import com.payhub.settlement.dto.AgentProfitRuleVO;
import com.payhub.settlement.entity.AgentProfitRule;
import com.payhub.settlement.enums.AgentSettleTypeEnum;
import com.payhub.settlement.mapper.AgentProfitRuleMapper;
import com.payhub.settlement.service.AgentProfitRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AgentProfitRuleServiceImpl extends ServiceImpl<AgentProfitRuleMapper, AgentProfitRule> implements AgentProfitRuleService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRule(AgentProfitRuleSaveRequest request) {
        AgentProfitRule rule;
        if (request.getId() != null) {
            rule = this.getById(request.getId());
            if (rule == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "分润规则不存在");
            }
        } else {
            rule = new AgentProfitRule();
            rule.setRuleNo(OrderNoGenerator.generateWithPrefix("APR"));
        }

        rule.setRuleName(request.getRuleName());
        rule.setMerchantNo(request.getMerchantNo());
        rule.setMerchantName(request.getMerchantName());
        rule.setAgentLevel(request.getAgentLevel());
        rule.setCommissionRate(request.getCommissionRate());
        rule.setMinCommission(request.getMinCommission());
        rule.setSettleType(request.getSettleType() != null ? request.getSettleType() : 0);
        rule.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        rule.setRemark(request.getRemark());

        this.saveOrUpdate(rule);
        log.info("分润规则保存成功: id={}, ruleNo={}, merchantNo={}", rule.getId(), rule.getRuleNo(), request.getMerchantNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        AgentProfitRule rule = this.getById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分润规则不存在");
        }
        this.removeById(id);
        log.info("分润规则删除成功: id={}", id);
    }

    @Override
    public AgentProfitRuleVO getRuleById(Long id) {
        AgentProfitRule rule = this.getById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分润规则不存在");
        }
        return convertToVO(rule);
    }

    @Override
    public AgentProfitRuleVO getRuleByRuleNo(String ruleNo) {
        AgentProfitRule rule = baseMapper.selectByRuleNo(ruleNo);
        return rule != null ? convertToVO(rule) : null;
    }

    @Override
    public List<AgentProfitRuleVO> listByMerchantNo(String merchantNo) {
        List<AgentProfitRule> rules = baseMapper.selectByMerchantNo(merchantNo);
        return rules.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public IPage<AgentProfitRuleVO> listPage(Long current, Long size, Map<String, Object> params) {
        Page<AgentProfitRule> page = new Page<>(current, size);
        IPage<AgentProfitRule> rulePage = baseMapper.selectPageList(page, params);
        return rulePage.convert(this::convertToVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleRule(Long id) {
        AgentProfitRule rule = this.getById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分润规则不存在");
        }
        rule.setStatus(rule.getStatus() == 1 ? 0 : 1);
        this.updateById(rule);
        log.info("分润规则状态切换成功: id={}, status={}", id, rule.getStatus());
    }

    private AgentProfitRuleVO convertToVO(AgentProfitRule rule) {
        AgentProfitRuleVO vo = BeanUtil.copyProperties(rule, AgentProfitRuleVO.class);
        vo.setStatusDesc(rule.getStatus() == 1 ? "启用" : "禁用");
        AgentSettleTypeEnum settleTypeEnum = AgentSettleTypeEnum.getByCode(rule.getSettleType());
        vo.setSettleTypeDesc(settleTypeEnum != null ? settleTypeEnum.getDesc() : "");
        return vo;
    }
}
