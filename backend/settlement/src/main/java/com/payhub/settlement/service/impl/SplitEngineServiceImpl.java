package com.payhub.settlement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.JsonUtils;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.pay.entity.PayOrder;
import com.payhub.settlement.entity.PaySplitDetail;
import com.payhub.settlement.entity.PaySplitRule;
import com.payhub.settlement.mapper.PaySplitDetailMapper;
import com.payhub.settlement.service.SplitEngineService;
import com.payhub.settlement.service.SplitRuleService;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SplitEngineServiceImpl extends ServiceImpl<PaySplitDetailMapper, PaySplitDetail> implements SplitEngineService {

    @Autowired
    private SplitRuleService splitRuleService;

    @Autowired(required = false)
    private SplitReceiverService splitReceiverService;

    @Override
    public List<PaySplitDetail> calculateSplit(PayOrder order, PaySplitRule rule) {
        List<PaySplitDetail> details = new ArrayList<>();

        String splitDetailsJson = rule.getSplitDetails();
        if (splitDetailsJson == null || splitDetailsJson.isEmpty()) {
            log.warn("分账规则明细为空, ruleNo={}", rule.getRuleNo());
            return details;
        }

        JSONArray detailArray = JsonUtils.parseArray(splitDetailsJson);
        if (detailArray == null || detailArray.isEmpty()) {
            log.warn("分账规则明细解析失败, ruleNo={}", rule.getRuleNo());
            return details;
        }

        BigDecimal orderAmount = order.getPayAmount();
        BigDecimal totalSplitAmount = BigDecimal.ZERO;

        for (int i = 0; i < detailArray.size(); i++) {
            JSONObject item = detailArray.getJSONObject(i);
            String receiverAccount = item.getString("receiverAccount");
            String receiverName = item.getString("receiverName");
            String splitType = item.getString("splitType");
            BigDecimal splitValue = item.getBigDecimal("splitValue");

            if (receiverAccount == null || splitType == null || splitValue == null) {
                log.warn("分账明细项参数不完整, 跳过: {}", item);
                continue;
            }

            if (splitReceiverService != null && !"REMAINING".equalsIgnoreCase(splitType)) {
                try {
                    splitReceiverService.checkReceiverVerified(receiverAccount, order.getMerchantNo());
                } catch (BusinessException e) {
                    log.error("分账接收方未通过实名认证校验, receiverAccount={}, error={}", receiverAccount, e.getMessage());
                    throw new BusinessException(ResultCode.PARAM_ERROR,
                            "分账接收方[" + (receiverName != null ? receiverName : receiverAccount) + "]未完成实名认证: " + e.getMessage());
                }
            }

            PaySplitDetail detail = new PaySplitDetail();
            detail.setSplitDetailNo(OrderNoGenerator.generateWithPrefix("SD"));
            detail.setOrderNo(order.getOrderNo());
            detail.setMerchantNo(order.getMerchantNo());
            detail.setRuleNo(rule.getRuleNo());
            detail.setReceiverAccount(receiverAccount);
            detail.setReceiverName(receiverName);
            detail.setSplitType(splitType);
            detail.setSplitValue(splitValue);
            detail.setStatus(0);

            BigDecimal splitAmount;
            if ("PERCENT".equalsIgnoreCase(splitType)) {
                splitAmount = orderAmount.multiply(splitValue).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
            } else if ("FIXED".equalsIgnoreCase(splitType)) {
                splitAmount = splitValue;
            } else {
                log.warn("未知的分账类型: {}, 跳过", splitType);
                continue;
            }

            detail.setSplitAmount(splitAmount);
            totalSplitAmount = totalSplitAmount.add(splitAmount);
            details.add(detail);
        }

        if (totalSplitAmount.compareTo(orderAmount) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "分账总金额不能超过订单金额");
        }

        BigDecimal remainingAmount = orderAmount.subtract(totalSplitAmount);
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            PaySplitDetail merchantDetail = new PaySplitDetail();
            merchantDetail.setSplitDetailNo(OrderNoGenerator.generateWithPrefix("SD"));
            merchantDetail.setOrderNo(order.getOrderNo());
            merchantDetail.setMerchantNo(order.getMerchantNo());
            merchantDetail.setRuleNo(rule.getRuleNo());
            merchantDetail.setReceiverAccount(order.getMerchantNo());
            merchantDetail.setReceiverName(order.getMerchantNo());
            merchantDetail.setSplitType("REMAINING");
            merchantDetail.setSplitValue(remainingAmount);
            merchantDetail.setSplitAmount(remainingAmount);
            merchantDetail.setStatus(0);
            merchantDetail.setRemark("剩余金额");
            details.add(merchantDetail);
        }

        return details;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PaySplitDetail> executeSplit(PayOrder order) {
        log.info("开始执行分账, orderNo={}, merchantNo={}", order.getOrderNo(), order.getMerchantNo());

        List<PaySplitRule> rules = splitRuleService.listByMerchantNo(order.getMerchantNo());
        if (rules == null || rules.isEmpty()) {
            log.info("商户无分账规则, merchantNo={}", order.getMerchantNo());
            return new ArrayList<>();
        }

        PaySplitRule enabledRule = null;
        for (PaySplitRule rule : rules) {
            if (rule.getStatus() != null && rule.getStatus() == 1) {
                enabledRule = rule;
                break;
            }
        }

        if (enabledRule == null) {
            log.info("商户无启用的分账规则, merchantNo={}", order.getMerchantNo());
            return new ArrayList<>();
        }

        List<PaySplitDetail> details = calculateSplit(order, enabledRule);
        this.saveBatch(details);

        log.info("分账执行完成, orderNo={}, 生成明细数量: {}", order.getOrderNo(), details.size());
        return details;
    }

    @Override
    public List<PaySplitDetail> calculateBatchSplit(Long settlementId, String settlementNo, List<PayOrder> orders) {
        List<PaySplitDetail> allDetails = new ArrayList<>();

        for (PayOrder order : orders) {
            List<PaySplitRule> rules = splitRuleService.listByMerchantNo(order.getMerchantNo());
            if (rules == null || rules.isEmpty()) {
                continue;
            }

            PaySplitRule enabledRule = null;
            for (PaySplitRule rule : rules) {
                if (rule.getStatus() != null && rule.getStatus() == 1) {
                    enabledRule = rule;
                    break;
                }
            }

            if (enabledRule == null) {
                continue;
            }

            List<PaySplitDetail> orderDetails = calculateSplit(order, enabledRule);
            for (PaySplitDetail detail : orderDetails) {
                detail.setSettlementId(settlementId);
                detail.setSettlementNo(settlementNo);
            }
            allDetails.addAll(orderDetails);
        }

        return allDetails;
    }

    @Override
    public List<PaySplitDetail> getSplitDetailsByOrderNo(String orderNo) {
        return this.baseMapper.selectByOrderNo(orderNo);
    }

    @Override
    public List<PaySplitDetail> getSplitDetailsBySettlementId(Long settlementId) {
        return this.baseMapper.selectBySettlementId(settlementId);
    }

    @Override
    public IPage<PaySplitDetail> listSplitDetails(Long current, Long size, Map<String, Object> params) {
        Page<PaySplitDetail> page = new Page<>(current, size);
        LambdaQueryWrapper<PaySplitDetail> wrapper = new LambdaQueryWrapper<>();
        if (params != null) {
            if (params.get("orderNo") != null) {
                wrapper.eq(PaySplitDetail::getOrderNo, params.get("orderNo"));
            }
            if (params.get("merchantNo") != null) {
                wrapper.eq(PaySplitDetail::getMerchantNo, params.get("merchantNo"));
            }
            if (params.get("ruleNo") != null) {
                wrapper.eq(PaySplitDetail::getRuleNo, params.get("ruleNo"));
            }
            if (params.get("status") != null) {
                wrapper.eq(PaySplitDetail::getStatus, params.get("status"));
            }
            if (params.get("receiverAccount") != null) {
                wrapper.eq(PaySplitDetail::getReceiverAccount, params.get("receiverAccount"));
            }
            if (params.get("settlementId") != null) {
                wrapper.eq(PaySplitDetail::getSettlementId, params.get("settlementId"));
            }
        }
        wrapper.orderByDesc(PaySplitDetail::getId);
        return this.page(page, wrapper);
    }

    @Override
    public boolean updateStatusBySettlementId(Long settlementId, Integer status) {
        int rows = this.baseMapper.updateStatusBySettlementId(settlementId, status, LocalDateTime.now());
        return rows > 0;
    }
}
