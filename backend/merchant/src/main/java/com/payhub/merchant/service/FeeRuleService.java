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
import com.payhub.merchant.entity.MerchantInfo;
import com.payhub.merchant.mapper.FeeRuleMapper;
import com.payhub.merchant.mapper.MerchantInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal DEFAULT_MAX_AMOUNT = new BigDecimal("99999999.99");

    private static final Map<String, String> CHANNEL_DESC = new HashMap<>();

    static {
        CHANNEL_DESC.put("ALIPAY", "支付宝");
        CHANNEL_DESC.put("WECHAT", "微信支付");
        CHANNEL_DESC.put("UNIONPAY", "银联");
        CHANNEL_DESC.put("APPLE_PAY", "Apple Pay");
        CHANNEL_DESC.put("GOOGLE_PAY", "Google Pay");
        CHANNEL_DESC.put("PAYPAL", "PayPal");
    }

    @Autowired
    private MerchantInfoMapper merchantInfoMapper;

    @Transactional(rollbackFor = Exception.class)
    public void saveRule(FeeRuleSaveRequest request) {
        BigDecimal minAmt = request.getMinAmount() != null ? request.getMinAmount() : BigDecimal.ZERO;
        BigDecimal maxAmt = request.getMaxAmount() != null ? request.getMaxAmount() : DEFAULT_MAX_AMOUNT;

        if (minAmt.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "金额区间最小值不能小于0");
        }
        if (maxAmt.compareTo(minAmt) <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "金额区间最大值必须大于最小值");
        }
        BigDecimal rate = request.getFeeRate();
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(HUNDRED) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "费率必须在0到100之间(百分比)");
        }
        BigDecimal minFee = request.getMinFee();
        BigDecimal maxFee = request.getMaxFee();
        if (minFee != null && minFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "最低手续费不能小于0");
        }
        if (maxFee != null && minFee != null && maxFee.compareTo(minFee) < 0) {
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
                    .eq(FeeRule::getMinAmount, minAmt)
                    .eq(FeeRule::getMaxAmount, maxAmt)
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
        rule.setPayChannel(StrUtil.isBlank(request.getPayChannel()) ? null : request.getPayChannel());
        rule.setMinAmount(minAmt);
        rule.setMaxAmount(maxAmt);
        rule.setFeeRate(rate);
        rule.setMinFee(minFee != null ? minFee : BigDecimal.ZERO);
        rule.setMaxFee(maxFee);
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

    public FeeRuleVO getByRuleNo(String ruleNo) {
        FeeRule rule = this.lambdaQuery().eq(FeeRule::getRuleNo, ruleNo).one();
        if (rule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "规则不存在");
        }
        return convertToVO(rule);
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
        String industryCode = request.getIndustryCode();
        if (StrUtil.isBlank(industryCode) && StrUtil.isNotBlank(request.getMerchantNo())) {
            MerchantInfo merchant = merchantInfoMapper.selectOne(
                    new LambdaQueryWrapper<MerchantInfo>()
                            .eq(MerchantInfo::getMerchantNo, request.getMerchantNo())
                            .last("LIMIT 1")
            );
            if (merchant != null && StrUtil.isNotBlank(merchant.getIndustryCode())) {
                industryCode = merchant.getIndustryCode();
            }
        }

        if (StrUtil.isBlank(industryCode)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "行业编码不能为空");
        }
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "交易金额必须大于0");
        }

        LambdaQueryWrapper<FeeRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeeRule::getIndustryCode, industryCode);
        if (StrUtil.isNotBlank(request.getPayChannel())) {
            wrapper.and(w -> w.eq(FeeRule::getPayChannel, request.getPayChannel()).or().isNull(FeeRule::getPayChannel));
        }
        wrapper.eq(FeeRule::getStatus, 1);
        wrapper.le(FeeRule::getMinAmount, amount);
        wrapper.ge(FeeRule::getMaxAmount, amount);
        wrapper.orderByDesc(FeeRule::getPriority);

        List<FeeRule> rules = this.list(wrapper);
        FeeRule matched = rules.stream()
                .filter(r -> amount.compareTo(r.getMinAmount()) >= 0 && amount.compareTo(r.getMaxAmount()) <= 0)
                .max(Comparator.comparingInt(FeeRule::getPriority))
                .orElse(null);

        FeeCalcResult result = new FeeCalcResult();
        result.setAmount(amount);
        result.setIndustryCode(industryCode);

        if (matched == null) {
            result.setFeeAmount(BigDecimal.ZERO);
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

        BigDecimal rawFee = amount.multiply(matched.getFeeRate())
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal fee = rawFee;

        String detail;
        BigDecimal minFee = matched.getMinFee();
        BigDecimal maxFee = matched.getMaxFee();

        if (minFee != null && minFee.compareTo(BigDecimal.ZERO) > 0 && fee.compareTo(minFee) < 0) {
            detail = String.format("金额%s × 费率%s%% = %s元，低于最低手续费%s元，取最低手续费",
                    amount, matched.getFeeRate(), rawFee.toPlainString(), minFee.toPlainString());
            fee = minFee;
        } else if (maxFee != null && maxFee.compareTo(BigDecimal.ZERO) > 0 && fee.compareTo(maxFee) > 0) {
            detail = String.format("金额%s × 费率%s%% = %s元，高于最高手续费%s元，取最高手续费",
                    amount, matched.getFeeRate(), rawFee.toPlainString(), maxFee.toPlainString());
            fee = maxFee;
        } else {
            detail = String.format("金额%s × 费率%s%% = %s元",
                    amount, matched.getFeeRate(), fee.toPlainString());
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
