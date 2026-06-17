package com.payhub.risk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.risk.engine.DroolsRuleEngine;
import com.payhub.risk.entity.RiskRule;
import com.payhub.risk.mapper.RiskRuleMapper;
import com.payhub.risk.service.RiskRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class RiskRuleServiceImpl extends ServiceImpl<RiskRuleMapper, RiskRule> implements RiskRuleService {

    @Autowired
    private DroolsRuleEngine droolsRuleEngine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(RiskRule rule) {
        validateRule(rule);

        if (StrUtil.isBlank(rule.getRuleCode())) {
            rule.setRuleCode("RULE_" + System.currentTimeMillis());
        }

        RiskRule existRule = this.getOne(new LambdaQueryWrapper<RiskRule>()
                .eq(RiskRule::getRuleCode, rule.getRuleCode())
                .eq(RiskRule::getDeleted, 0));
        if (existRule != null) {
            throw new BusinessException("规则编码已存在");
        }

        if (rule.getStatus() == null) {
            rule.setStatus(0);
        }
        if (rule.getPriority() == null) {
            rule.setPriority(100);
        }
        if (rule.getRiskLevel() == null) {
            rule.setRiskLevel(1);
        }
        if (rule.getActionType() == null) {
            rule.setActionType("BLOCK");
        }

        String drlContent = droolsRuleEngine.buildRuleContent(rule);
        rule.setRuleContent(drlContent);

        this.save(rule);
        log.info("保存规则成功，规则编码：{}，规则名称：{}", rule.getRuleCode(), rule.getRuleName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(RiskRule rule) {
        if (rule.getId() == null) {
            throw new BusinessException("规则ID不能为空");
        }

        RiskRule existRule = this.getById(rule.getId());
        if (existRule == null || existRule.getDeleted() == 1) {
            throw new BusinessException("规则不存在");
        }

        if (StrUtil.isNotBlank(rule.getRuleCode()) && !rule.getRuleCode().equals(existRule.getRuleCode())) {
            RiskRule sameCodeRule = this.getOne(new LambdaQueryWrapper<RiskRule>()
                    .eq(RiskRule::getRuleCode, rule.getRuleCode())
                    .eq(RiskRule::getDeleted, 0)
                    .ne(RiskRule::getId, rule.getId()));
            if (sameCodeRule != null) {
                throw new BusinessException("规则编码已存在");
            }
        }

        String drlContent = droolsRuleEngine.buildRuleContent(rule);
        rule.setRuleContent(drlContent);

        this.updateById(rule);
        log.info("更新规则成功，规则ID：{}", rule.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        RiskRule rule = this.getById(id);
        if (rule == null || rule.getDeleted() == 1) {
            throw new BusinessException("规则不存在");
        }

        this.removeById(id);
        log.info("删除规则成功，规则ID：{}", id);
    }

    @Override
    public RiskRule getById(Long id) {
        RiskRule rule = super.getById(id);
        if (rule == null || rule.getDeleted() == 1) {
            return null;
        }
        return rule;
    }

    @Override
    public IPage<RiskRule> listPage(Long current, Long size, Map<String, Object> params) {
        LambdaQueryWrapper<RiskRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskRule::getDeleted, 0);

        if (params != null) {
            if (params.get("ruleCode") != null && StrUtil.isNotBlank(params.get("ruleCode").toString())) {
                wrapper.like(RiskRule::getRuleCode, params.get("ruleCode").toString());
            }
            if (params.get("ruleName") != null && StrUtil.isNotBlank(params.get("ruleName").toString())) {
                wrapper.like(RiskRule::getRuleName, params.get("ruleName").toString());
            }
            if (params.get("ruleType") != null && StrUtil.isNotBlank(params.get("ruleType").toString())) {
                wrapper.eq(RiskRule::getRuleType, params.get("ruleType").toString());
            }
            if (params.get("riskLevel") != null) {
                wrapper.eq(RiskRule::getRiskLevel, Integer.parseInt(params.get("riskLevel").toString()));
            }
            if (params.get("status") != null) {
                wrapper.eq(RiskRule::getStatus, Integer.parseInt(params.get("status").toString()));
            }
        }

        wrapper.orderByAsc(RiskRule::getPriority);
        wrapper.orderByDesc(RiskRule::getCreatedAt);

        return this.page(new Page<>(current, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableRule(Long id) {
        RiskRule rule = this.getById(id);
        if (rule == null) {
            throw new BusinessException("规则不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        if (rule.getEffectStartTime() != null && rule.getEffectStartTime().isAfter(now)) {
            throw new BusinessException("规则尚未生效，无法启用");
        }
        if (rule.getEffectEndTime() != null && rule.getEffectEndTime().isBefore(now)) {
            throw new BusinessException("规则已过期，无法启用");
        }

        rule.setStatus(1);
        this.updateById(rule);
        log.info("启用规则成功，规则ID：{}", id);

        this.reloadAllRules();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableRule(Long id) {
        RiskRule rule = this.getById(id);
        if (rule == null) {
            throw new BusinessException("规则不存在");
        }

        rule.setStatus(0);
        this.updateById(rule);
        log.info("禁用规则成功，规则ID：{}", id);

        this.reloadAllRules();
    }

    @Override
    public void reloadAllRules() {
        try {
            droolsRuleEngine.reloadRules();
            log.info("重新加载所有规则成功");
        } catch (Exception e) {
            log.error("重新加载所有规则失败", e);
            throw new BusinessException("重新加载规则失败：" + e.getMessage());
        }
    }

    @Override
    public String generateRuleContent(Long id) {
        RiskRule rule = this.getById(id);
        if (rule == null) {
            throw new BusinessException("规则不存在");
        }

        try {
            return droolsRuleEngine.buildRuleContent(rule);
        } catch (Exception e) {
            log.error("生成规则内容失败，规则ID：{}", id, e);
            throw new BusinessException("生成规则内容失败：" + e.getMessage());
        }
    }

    private void validateRule(RiskRule rule) {
        if (StrUtil.isBlank(rule.getRuleName())) {
            throw new BusinessException("规则名称不能为空");
        }
        if (StrUtil.isBlank(rule.getRuleType())) {
            throw new BusinessException("规则类型不能为空");
        }
        if (StrUtil.isBlank(rule.getRuleCondition())) {
            throw new BusinessException("规则条件不能为空");
        }
    }
}
