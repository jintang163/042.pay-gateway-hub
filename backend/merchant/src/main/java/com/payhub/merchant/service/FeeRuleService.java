package com.payhub.merchant.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.merchant.dto.*;
import com.payhub.merchant.entity.FeeRule;
import com.payhub.merchant.mapper.FeeRuleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FeeRuleService extends ServiceImpl<FeeRuleMapper, FeeRule> {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Map<String, String> CHANNEL_DESC = new HashMap<>();

    static {
        CHANNEL_DESC.put("ALIPAY", "支付宝");
        CHANNEL_DESC.put("WECHAT", "微信支付");
        CHANNEL_DESC.put("UNIONPAY", "银联");
        CHANNEL_DESC.put("APPLE_PAY", "Apple Pay");
        CHANNEL_DESC.put("GOOGLE_PAY", "Google Pay");
        CHANNEL_DESC.put("PAYPAL", "PayPal");
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveRule(FeeRuleSaveRequest request) {
        if (request.getMinAmount() < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "金额区间最小值不能小于0");
        }
        if (request.getMaxAmount() <= request.getMinAmount()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "金额区间最大值必须大于最小值");
        }
        if (request.getFeeRate().compareTo(BigDecimal.ZERO) < 0 || request.getFeeRate().compareTo(new BigDecimal("1")) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "费率必须在0到1之间");
        }
        if (request.getMinFee() != null && request.getMinFee() < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "最低手续费不能小于0");
        }
        if (request.getMaxFee() != null && request.getMinFee() != null && request.getMaxFee() < request.getMinFee()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "最高手续费不能小于最低手续费");
        }

        FeeRule rule;
        if (request.getId() != null) {
            rule = this.getById(request.getId());
            if (rule == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "规则不存在");
            }
        } else {
            LambdaQueryWrapper<FeeRule> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FeeRule::getIndustryCode, request.getIndustryCode())
                    .and(w -> w.eq(FeeRule::getPayChannel, request.getPayChannel()).or().isNull(FeeRule::getPayChannel))
                    .eq(FeeRule::getMinAmount, request.getMinAmount())
                    .eq(FeeRule::getMaxAmount, request.getMaxAmount())
                    .last("LIMIT 1");
            FeeRule exist = this.getOne(wrapper);
            if (exist != null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "该行业渠道与金额区间的规则已存在");
            }
            rule = new FeeRule();
            rule.setRuleNo("FR" + System.currentTimeMillis() + RandomUtil.randomNumbers(4));
        }

        rule.setIndustryCode(request.getIndustryCode());
        rule.setIndustryName(request.getIndustryName());
        rule.setPayChannel(request.getPayChannel());
        rule.setMinAmount(request.getMinAmount());
        rule.setMaxAmount(request.getMaxAmount());
        rule.setFeeRate(request.getFeeRate());
        rule.setMinFee(request.getMinFee() != null ? request.getMinFee() : 0L);
        rule.setMaxFee(request.getMaxFee());
        rule.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        rule.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        rule.setRemark(request.getRemark());

        this.saveOrUpdate(rule);
    }

    public IPage<FeeRuleVO> listPage(int current, int size, String industryCode, String payChannel, Integer status) {
        LambdaQueryWrapper<FeeRule> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(industryCode)) {
            wrapper.eq(FeeRule::getIndustryCode, industryCode);
        }
        if (StrUtil.isNotBlank(payChannel)) {
            wrapper.and(w -> w.eq(FeeRule::getPayChannel, payChannel).or().isNull(FeeRule::getPayChannel));
        }
        if (status != null) {
            wrapper.eq(FeeRule::getStatus, status);
        }
        wrapper.orderByDesc(FeeRule::getPriority).orderByDesc(FeeRule::getCreatedAt);

        IPage<FeeRule> page = this.page(new Page<>(current, size), wrapper);
        IPage<FeeRuleVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setPages(page.getPages());
        List<FeeRuleVO> voList = page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    public List<FeeRuleVO> getByRuleNo(String ruleNo) {
        FeeRule rule = this.lambdaQuery().eq(FeeRule::getRuleNo, ruleNo).one();
        if (rule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "规则不存在");
        }
        return java.util.Collections.singletonList(convertToVO(rule));
    }

    public List<FeeRuleVO> listByIndustry(String industryCode, String payChannel) {
        LambdaQueryWrapper<FeeRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeeRule::getIndustryCode, industryCode);
        if (StrUtil.isNotBlank(payChannel)) {
            wrapper.and(w -> w.eq(FeeRule::getPayChannel, payChannel).or().isNull(FeeRule::getPayChannel));
        }
        wrapper.eq(FeeRule::getStatus, 1);
        wrapper.orderByDesc(FeeRule::getPriority).orderByAsc(FeeRule::getMinAmount);
        return this.list(wrapper).stream().map(this::convertToVO).collect(Collectors.toList());
    }

    public FeeCalcResult calculate(FeeCalcRequest request) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "交易金额必须大于0");
        }
        LambdaQueryWrapper<FeeRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeeRule::getIndustryCode, request.getIndustryCode());
        if (StrUtil.isNotBlank(request.getPayChannel())) {
            wrapper.and(w -> w.eq(FeeRule::getPayChannel, request.getPayChannel()).or().isNull(FeeRule::getPayChannel));
        }
        wrapper.eq(FeeRule::getStatus, 1);
        wrapper.le(FeeRule::getMinAmount, request.getAmount());
        wrapper.ge(FeeRule::getMaxAmount, request.getAmount());
        wrapper.orderByDesc(FeeRule::getPriority);

        List<FeeRule> rules = this.list(wrapper);
        FeeRule matched = rules.stream()
                .filter(r -> request.getAmount() >= r.getMinAmount() && request.getAmount() <= r.getMaxAmount())
                .max(Comparator.comparingInt(FeeRule::getPriority))
                .orElse(null);

        FeeCalcResult result = new FeeCalcResult();
        result.setAmount(request.getAmount());
        result.setIndustryCode(request.getIndustryCode());

        if (matched == null) {
            result.setFeeAmount(0L);
            result.setFeeRate(BigDecimal.ZERO);
            result.setCalcDetail("未匹配到规则，按0手续费处理");
            return result;
        }

        result.setRuleNo(matched.getRuleNo());
        result.setIndustryName(matched.getIndustryName());
        result.setPayChannel(matched.getPayChannel());
        result.setFeeRate(matched.getFeeRate());
        result.setMinFee(matched.getMinFee());
        result.setMaxFee(matched.getMaxFee());

        BigDecimal amount = BigDecimal.valueOf(request.getAmount());
        BigDecimal rawFee = amount.multiply(matched.getFeeRate()).setScale(0, RoundingMode.HALF_UP);
        long fee = rawFee.longValue();

        String detail;
        if (matched.getMinFee() != null && matched.getMinFee() > 0 && fee < matched.getMinFee()) {
            detail = String.format("金额%s × 费率%s = %s分，低于最低手续费%s分，取最低手续费", request.getAmount(), matched.getFeeRate(), rawFee.longValue(), matched.getMinFee());
            fee = matched.getMinFee();
        } else if (matched.getMaxFee() != null && matched.getMaxFee() > 0 && fee > matched.getMaxFee()) {
            detail = String.format("金额%s × 费率%s = %s分，高于最高手续费%s分，取最高手续费", request.getAmount(), matched.getFeeRate(), rawFee.longValue(), matched.getMaxFee());
            fee = matched.getMaxFee();
        } else {
            detail = String.format("金额%s × 费率%s = %s分", request.getAmount(), matched.getFeeRate(), fee);
        }

        result.setFeeAmount(fee);
        result.setCalcDetail(detail);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        FeeRule rule = this.getById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "规则不存在");
        }
        rule.setStatus(rule.getStatus() == 1 ? 0 : 1);
        this.updateById(rule);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        FeeRule rule = this.getById(id);
        if (rule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "规则不存在");
        }
        this.removeById(id);
    }

    public List<Map<String, String>> listIndustries() {
        List<FeeRule> all = this.lambdaQuery()
                .select(FeeRule::getIndustryCode, FeeRule::getIndustryName)
                .groupBy(FeeRule::getIndustryCode, FeeRule::getIndustryName)
                .list();
        Map<String, String> distinct = new HashMap<>();
        for (FeeRule r : all) {
            distinct.put(r.getIndustryCode(), r.getIndustryName());
        }
        return distinct.entrySet().stream().map(e -> {
            Map<String, String> m = new HashMap<>();
            m.put("code", e.getKey());
            m.put("name", e.getValue());
            return m;
        }).collect(Collectors.toList());
    }

    private FeeRuleVO convertToVO(FeeRule rule) {
        FeeRuleVO vo = BeanUtil.copyProperties(rule, FeeRuleVO.class);
        String channel = rule.getPayChannel();
        if (StrUtil.isBlank(channel)) {
            vo.setPayChannelDesc("全部渠道");
        } else {
            vo.setPayChannelDesc(CHANNEL_DESC.getOrDefault(channel, channel));
        }
        vo.setStatusDesc(rule.getStatus() == 1 ? "启用" : "禁用");
        LocalDateTime createdAt = rule.getCreatedAt();
        if (createdAt != null) {
            vo.setCreatedAt(createdAt.format(DTF));
        }
        return vo;
    }
}
