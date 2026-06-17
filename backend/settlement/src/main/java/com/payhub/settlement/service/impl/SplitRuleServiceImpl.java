package com.payhub.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.settlement.dto.SplitRuleSaveRequest;
import com.payhub.settlement.dto.SplitRuleVO;
import com.payhub.settlement.entity.PaySplitRule;
import com.payhub.settlement.mapper.PaySplitRuleMapper;
import com.payhub.settlement.service.SplitRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SplitRuleServiceImpl extends ServiceImpl<PaySplitRuleMapper, PaySplitRule> implements SplitRuleService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRule(SplitRuleSaveRequest request) {
        PaySplitRule rule;
        if (request.getId() != null) {
            rule = this.getById(request.getId());
            if (rule == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "分账规则不存在");
            }
        } else {
            rule = new PaySplitRule();
            rule.setRuleNo(OrderNoGenerator.generateWithPrefix("SR"));
        }

        rule.setMerchantNo(request.getMerchantNo());
        rule.setRuleName(request.getRuleName());
        rule.setSplitDetails(request.getSplitDetails());
        rule.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        this.saveOrUpdate(rule);
        log.info("分账规则保存成功: id={}, ruleNo={}, merchantNo={}", rule.getId(), rule.getRuleNo(), request.getMerchantNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        PaySplitRule rule = this.getById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分账规则不存在");
        }
        this.removeById(id);
        log.info("分账规则删除成功: id={}", id);
    }

    @Override
    public SplitRuleVO getRuleById(Long id) {
        PaySplitRule rule = this.getById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分账规则不存在");
        }
        return convertToVO(rule);
    }

    @Override
    public List<SplitRuleVO> listByMerchantNo(String merchantNo) {
        LambdaQueryWrapper<PaySplitRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaySplitRule::getMerchantNo, merchantNo);
        List<PaySplitRule> rules = this.list(wrapper);
        return rules.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public IPage<SplitRuleVO> listPage(Long current, Long size, Map<String, Object> params) {
        Page<PaySplitRule> page = new Page<>(current, size);
        LambdaQueryWrapper<PaySplitRule> wrapper = new LambdaQueryWrapper<>();
        if (params != null) {
            if (params.get("merchantNo") != null) {
                wrapper.eq(PaySplitRule::getMerchantNo, params.get("merchantNo"));
            }
            if (params.get("ruleName") != null) {
                wrapper.like(PaySplitRule::getRuleName, params.get("ruleName"));
            }
            if (params.get("status") != null) {
                wrapper.eq(PaySplitRule::getStatus, params.get("status"));
            }
        }
        wrapper.orderByDesc(PaySplitRule::getId);
        IPage<PaySplitRule> rulePage = this.page(page, wrapper);
        return rulePage.convert(this::convertToVO);
    }

    private SplitRuleVO convertToVO(PaySplitRule rule) {
        SplitRuleVO vo = BeanUtil.copyProperties(rule, SplitRuleVO.class);
        vo.setStatusDesc(rule.getStatus() == 1 ? "启用" : "禁用");
        return vo;
    }
}
