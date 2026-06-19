package com.payhub.settlement.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.enums.PayChannelEnum;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.settlement.dto.AutoWriteoffRuleSaveRequest;
import com.payhub.settlement.dto.AutoWriteoffRuleVO;
import com.payhub.settlement.entity.ReconcileAutoWriteoffRule;
import com.payhub.settlement.entity.ReconcileDetail;
import com.payhub.settlement.enums.ReconcileDiffTypeEnum;
import com.payhub.settlement.enums.WriteoffTypeEnum;
import com.payhub.settlement.mapper.ReconcileAutoWriteoffRuleMapper;
import com.payhub.settlement.service.ReconcileAutoWriteoffRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReconcileAutoWriteoffRuleServiceImpl extends ServiceImpl<ReconcileAutoWriteoffRuleMapper, ReconcileAutoWriteoffRule>
        implements ReconcileAutoWriteoffRuleService {

    @Override
    public IPage<AutoWriteoffRuleVO> listPage(Long current, Long size, Map<String, Object> params) {
        LambdaQueryWrapper<ReconcileAutoWriteoffRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconcileAutoWriteoffRule::getDeleted, 0);

        if (params != null) {
            if (params.get("merchantNo") != null && StrUtil.isNotBlank(params.get("merchantNo").toString())) {
                wrapper.eq(ReconcileAutoWriteoffRule::getMerchantNo, params.get("merchantNo").toString());
            }
            if (params.get("payChannel") != null && StrUtil.isNotBlank(params.get("payChannel").toString())) {
                wrapper.eq(ReconcileAutoWriteoffRule::getPayChannel, params.get("payChannel").toString());
            }
            if (params.get("diffType") != null) {
                wrapper.eq(ReconcileAutoWriteoffRule::getDiffType, Integer.parseInt(params.get("diffType").toString()));
            }
            if (params.get("enabled") != null) {
                wrapper.eq(ReconcileAutoWriteoffRule::getEnabled, Integer.parseInt(params.get("enabled").toString()));
            }
        }

        wrapper.orderByDesc(ReconcileAutoWriteoffRule::getPriority, ReconcileAutoWriteoffRule::getId);

        IPage<ReconcileAutoWriteoffRule> page = this.page(new Page<>(current, size), wrapper);
        return page.convert(this::convertToVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRule(AutoWriteoffRuleSaveRequest request) {
        validateRule(request);

        ReconcileAutoWriteoffRule rule = new ReconcileAutoWriteoffRule();
        rule.setRuleName(request.getRuleName());
        rule.setMerchantNo(request.getMerchantNo());
        rule.setPayChannel(request.getPayChannel());
        rule.setDiffType(request.getDiffType());
        rule.setMaxAmount(request.getMaxAmount());
        rule.setAutoWriteoff(request.getAutoWriteoff() != null ? request.getAutoWriteoff() : 1);
        rule.setHandleType(request.getHandleType());
        rule.setEnabled(request.getEnabled() != null ? request.getEnabled() : 1);
        rule.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        rule.setRemark(request.getRemark());
        this.save(rule);

        log.info("添加自动平账规则成功，ruleId：{}，ruleName：{}", rule.getId(), rule.getRuleName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRule(AutoWriteoffRuleSaveRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "规则ID不能为空");
        }

        ReconcileAutoWriteoffRule rule = this.getById(request.getId());
        if (rule == null || rule.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "规则不存在");
        }

        validateRule(request);

        rule.setRuleName(request.getRuleName());
        rule.setMerchantNo(request.getMerchantNo());
        rule.setPayChannel(request.getPayChannel());
        rule.setDiffType(request.getDiffType());
        rule.setMaxAmount(request.getMaxAmount());
        rule.setAutoWriteoff(request.getAutoWriteoff());
        rule.setHandleType(request.getHandleType());
        rule.setEnabled(request.getEnabled());
        rule.setPriority(request.getPriority());
        rule.setRemark(request.getRemark());
        this.updateById(rule);

        log.info("更新自动平账规则成功，ruleId：{}", rule.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        ReconcileAutoWriteoffRule rule = this.getById(id);
        if (rule == null || rule.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "规则不存在");
        }

        this.removeById(id);
        log.info("删除自动平账规则成功，ruleId：{}", id);
    }

    @Override
    public ReconcileAutoWriteoffRule matchRule(ReconcileDetail detail) {
        if (detail == null || detail.getDiffType() == null) {
            return null;
        }

        BigDecimal diffAmount = detail.getDiffAmount();
        if (diffAmount == null) {
            diffAmount = BigDecimal.ZERO;
        }
        BigDecimal absDiffAmount = diffAmount.abs();

        LambdaQueryWrapper<ReconcileAutoWriteoffRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconcileAutoWriteoffRule::getEnabled, 1)
                .eq(ReconcileAutoWriteoffRule::getAutoWriteoff, 1)
                .eq(ReconcileAutoWriteoffRule::getDiffType, detail.getDiffType())
                .eq(ReconcileAutoWriteoffRule::getDeleted, 0);

        if (StrUtil.isNotBlank(detail.getMerchantNo())) {
            wrapper.and(w -> w
                    .eq(ReconcileAutoWriteoffRule::getMerchantNo, detail.getMerchantNo())
                    .or()
                    .isNull(ReconcileAutoWriteoffRule::getMerchantNo));
        } else {
            wrapper.isNull(ReconcileAutoWriteoffRule::getMerchantNo);
        }

        if (StrUtil.isNotBlank(detail.getPayChannel())) {
            wrapper.and(w -> w
                    .eq(ReconcileAutoWriteoffRule::getPayChannel, detail.getPayChannel())
                    .or()
                    .isNull(ReconcileAutoWriteoffRule::getPayChannel));
        } else {
            wrapper.isNull(ReconcileAutoWriteoffRule::getPayChannel);
        }

        wrapper.orderByDesc(ReconcileAutoWriteoffRule::getPriority);

        List<ReconcileAutoWriteoffRule> rules = this.list(wrapper);
        for (ReconcileAutoWriteoffRule rule : rules) {
            if (rule.getMaxAmount() != null && absDiffAmount.compareTo(rule.getMaxAmount()) <= 0) {
                log.info("匹配到自动平账规则，ruleId：{}，ruleName：{}，diffAmount：{}，maxAmount：{}",
                        rule.getId(), rule.getRuleName(), absDiffAmount, rule.getMaxAmount());
                return rule;
            }
        }

        return null;
    }

    private void validateRule(AutoWriteoffRuleSaveRequest request) {
        if (StrUtil.isBlank(request.getRuleName())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "规则名称不能为空");
        }
        if (request.getDiffType() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "差异类型不能为空");
        }
        if (request.getMaxAmount() == null || request.getMaxAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "最大平账金额必须大于0");
        }
        if (request.getHandleType() != null && WriteoffTypeEnum.getByCode(request.getHandleType()) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无效的平账处理方式");
        }
    }

    private AutoWriteoffRuleVO convertToVO(ReconcileAutoWriteoffRule entity) {
        AutoWriteoffRuleVO vo = new AutoWriteoffRuleVO();
        vo.setId(entity.getId());
        vo.setRuleName(entity.getRuleName());
        vo.setMerchantNo(entity.getMerchantNo());
        vo.setPayChannel(entity.getPayChannel());

        if (StrUtil.isNotBlank(entity.getPayChannel())) {
            PayChannelEnum channelEnum = PayChannelEnum.getByCode(entity.getPayChannel());
            vo.setPayChannelDesc(channelEnum != null ? channelEnum.getDesc() : entity.getPayChannel());
        }

        vo.setDiffType(entity.getDiffType());
        ReconcileDiffTypeEnum diffTypeEnum = ReconcileDiffTypeEnum.getByCode(entity.getDiffType());
        vo.setDiffTypeDesc(diffTypeEnum != null ? diffTypeEnum.getDesc() : "");

        vo.setMaxAmount(entity.getMaxAmount());
        vo.setAutoWriteoff(entity.getAutoWriteoff());
        vo.setAutoWriteoffDesc(entity.getAutoWriteoff() != null && entity.getAutoWriteoff() == 1 ? "是" : "否");

        vo.setHandleType(entity.getHandleType());
        if (entity.getHandleType() != null) {
            WriteoffTypeEnum typeEnum = WriteoffTypeEnum.getByCode(entity.getHandleType());
            vo.setHandleTypeDesc(typeEnum != null ? typeEnum.getDesc() : "");
        }

        vo.setEnabled(entity.getEnabled());
        vo.setEnabledDesc(entity.getEnabled() != null && entity.getEnabled() == 1 ? "启用" : "禁用");
        vo.setPriority(entity.getPriority());
        vo.setRemark(entity.getRemark());
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
