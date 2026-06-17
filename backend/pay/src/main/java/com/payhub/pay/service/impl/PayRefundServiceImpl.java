package com.payhub.pay.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.channel.dto.QueryRefundResponse;
import com.payhub.channel.entity.PayChannelConfig;
import com.payhub.channel.mapper.PayChannelConfigMapper;
import com.payhub.channel.strategy.PayChannelStrategy;
import com.payhub.channel.strategy.PayChannelStrategyFactory;
import com.payhub.common.enums.PayStatusEnum;
import com.payhub.common.enums.RefundStatusEnum;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.pay.dto.RefundRequest;
import com.payhub.pay.dto.RefundResponse;
import com.payhub.pay.entity.MerchantPayConfig;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.entity.PayRefund;
import com.payhub.pay.mapper.PayRefundMapper;
import com.payhub.pay.service.MerchantPayConfigService;
import com.payhub.pay.service.PayOrderService;
import com.payhub.pay.service.PayRefundService;
import com.payhub.risk.dto.RiskCheckRequest;
import com.payhub.risk.dto.RiskCheckResult;
import com.payhub.risk.service.RiskControlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PayRefundServiceImpl extends ServiceImpl<PayRefundMapper, PayRefund> implements PayRefundService {

    private static final int MAX_RETRY_COUNT = 5;
    private static final long BASE_RETRY_INTERVAL_MINUTES = 1;
    private static final String REFUND_RETRY_KEY_PREFIX = "pay:refund:retry:";

    @Autowired
    private PayOrderService payOrderService;

    @Autowired
    private RiskControlService riskControlService;

    @Autowired
    private PayChannelStrategyFactory payChannelStrategyFactory;

    @Autowired
    private PayChannelConfigMapper payChannelConfigMapper;

    @Autowired
    private MerchantPayConfigService merchantPayConfigService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundResponse applyRefund(RefundRequest request, String clientIp) {
        LambdaQueryWrapper<PayOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(PayOrder::getOrderNo, request.getOrderNo())
                .eq(PayOrder::getMerchantNo, request.getMerchantNo())
                .last("LIMIT 1");
        PayOrder order = payOrderService.getOne(orderWrapper);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "支付订单不存在");
        }
        if (!PayStatusEnum.SUCCESS.getCode().equals(order.getPayStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单状态不允许退款");
        }

        LambdaQueryWrapper<PayRefund> refundWrapper = new LambdaQueryWrapper<>();
        refundWrapper.eq(PayRefund::getMerchantNo, request.getMerchantNo())
                .eq(PayRefund::getMerchantRefundNo, request.getMerchantRefundNo())
                .last("LIMIT 1");
        PayRefund existRefund = this.getOne(refundWrapper);
        if (existRefund != null) {
            return RefundResponse.builder()
                    .refundNo(existRefund.getRefundNo())
                    .refundStatus(existRefund.getRefundStatus())
                    .build();
        }

        LambdaQueryWrapper<PayRefund> sumWrapper = new LambdaQueryWrapper<>();
        sumWrapper.eq(PayRefund::getOrderNo, request.getOrderNo())
                .eq(PayRefund::getRefundStatus, RefundStatusEnum.SUCCESS.getCode());
        BigDecimal totalRefunded = this.list(sumWrapper).stream()
                .map(PayRefund::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableRefund = order.getPayAmount().subtract(totalRefunded);
        if (request.getRefundAmount().compareTo(availableRefund) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "退款金额超过可退金额");
        }

        RiskCheckRequest riskCheckRequest = RiskCheckRequest.builder()
                .merchantNo(request.getMerchantNo())
                .orderNo(request.getOrderNo())
                .clientIp(clientIp)
                .userIdentity(order.getUserIdentity())
                .payAmount(request.getRefundAmount())
                .payChannel(order.getPayChannel())
                .payType(order.getPayType())
                .build();
        RiskCheckResult riskResult = riskControlService.checkRisk(riskCheckRequest);
        if (riskResult == null || !riskResult.getPass()) {
            String riskDesc = riskResult != null ? riskResult.getRiskDesc() : "风控校验失败";
            throw new BusinessException(ResultCode.PARAM_ERROR, "风控校验不通过：" + riskDesc);
        }
        log.info("退款风控校验通过: merchantNo={}, orderNo={}, riskLevel={}",
                request.getMerchantNo(), request.getOrderNo(), riskResult.getRiskLevel());

        String refundNo = OrderNoGenerator.generateRefund();

        PayRefund refund = new PayRefund();
        refund.setRefundNo(refundNo);
        refund.setOrderNo(request.getOrderNo());
        refund.setMerchantNo(request.getMerchantNo());
        refund.setMerchantRefundNo(request.getMerchantRefundNo());
        refund.setPayAmount(order.getPayAmount());
        refund.setRefundAmount(request.getRefundAmount());
        refund.setRefundReason(request.getRefundReason());
        refund.setRefundStatus(RefundStatusEnum.PROCESSING.getCode());
        refund.setRetryCount(0);

        this.save(refund);

        log.info("退款申请提交成功: refundNo={}, orderNo={}, amount={}", refundNo, request.getOrderNo(), request.getRefundAmount());

        try {
            MerchantPayConfig merchantConfig = getMerchantPayConfig(order);
            if (merchantConfig == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "未找到商户支付配置");
            }

            PayChannelConfig channelConfig = payChannelConfigMapper.selectByChannelCode(merchantConfig.getChannelCode());
            if (channelConfig == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "未找到通道配置");
            }

            com.payhub.channel.dto.RefundRequest channelRefundRequest = buildChannelRefundRequest(
                    refund, order, channelConfig);

            PayChannelStrategy strategy = payChannelStrategyFactory.getStrategy(merchantConfig.getChannelCode());
            com.payhub.channel.dto.RefundResponse channelResponse = strategy.refund(channelRefundRequest);

            refund.setChannelRefundNo(channelResponse.getChannelRefundNo());

            if (channelResponse.isSuccess()) {
                String refundStatus = channelResponse.getRefundStatus();
                if ("SUCCESS".equalsIgnoreCase(refundStatus)) {
                    refund.setRefundStatus(RefundStatusEnum.SUCCESS.getCode());
                    refund.setRefundTime(LocalDateTime.now());
                    log.info("退款成功: refundNo={}, channelRefundNo={}", refundNo, channelResponse.getChannelRefundNo());
                } else if ("FAIL".equalsIgnoreCase(refundStatus)) {
                    refund.setRefundStatus(RefundStatusEnum.FAIL.getCode());
                    calculateNextRetryTime(refund);
                    log.warn("退款失败: refundNo={}, msg={}", refundNo, channelResponse.getMsg());
                } else {
                    refund.setRefundStatus(RefundStatusEnum.PROCESSING.getCode());
                    log.info("退款处理中: refundNo={}, channelRefundNo={}", refundNo, channelResponse.getChannelRefundNo());
                }
            } else {
                refund.setRefundStatus(RefundStatusEnum.FAIL.getCode());
                calculateNextRetryTime(refund);
                log.warn("通道退款调用失败: refundNo={}, code={}, msg={}",
                        refundNo, channelResponse.getCode(), channelResponse.getMsg());
            }

            this.updateById(refund);

        } catch (Exception e) {
            log.error("调用通道退款异常: refundNo={}", refundNo, e);
            refund.setRefundStatus(RefundStatusEnum.FAIL.getCode());
            calculateNextRetryTime(refund);
            this.updateById(refund);
        }

        saveRetryInfoToRedis(refund);

        return RefundResponse.builder()
                .refundNo(refundNo)
                .refundStatus(refund.getRefundStatus())
                .channelRefundNo(refund.getChannelRefundNo())
                .build();
    }

    @Override
    public RefundResponse queryRefund(String refundNo) {
        LambdaQueryWrapper<PayRefund> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PayRefund::getRefundNo, refundNo)
                .last("LIMIT 1");
        PayRefund refund = this.getOne(wrapper);
        if (refund == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "退款订单不存在");
        }

        if (RefundStatusEnum.PROCESSING.getCode().equals(refund.getRefundStatus())
                && StrUtil.isNotBlank(refund.getChannelRefundNo())) {
            try {
                PayOrder order = payOrderService.getOrderDetail(refund.getOrderNo(), refund.getMerchantNo());
                MerchantPayConfig merchantConfig = getMerchantPayConfig(order);
                if (merchantConfig != null) {
                    PayChannelStrategy strategy = payChannelStrategyFactory.getStrategy(merchantConfig.getChannelCode());
                    QueryRefundResponse queryResponse = strategy.queryRefund(refund.getRefundNo(), refund.getChannelRefundNo());

                    if (queryResponse != null) {
                        String status = queryResponse.getRefundStatus();
                        if ("SUCCESS".equalsIgnoreCase(status)) {
                            refund.setRefundStatus(RefundStatusEnum.SUCCESS.getCode());
                            refund.setRefundTime(LocalDateTime.now());
                            this.updateById(refund);
                            log.info("退款查询更新为成功: refundNo={}", refundNo);
                        } else if ("FAIL".equalsIgnoreCase(status)) {
                            refund.setRefundStatus(RefundStatusEnum.FAIL.getCode());
                            calculateNextRetryTime(refund);
                            this.updateById(refund);
                            log.info("退款查询更新为失败: refundNo={}", refundNo);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("查询通道退款状态异常: refundNo={}", refundNo, e);
            }
        }

        return RefundResponse.builder()
                .refundNo(refund.getRefundNo())
                .refundStatus(refund.getRefundStatus())
                .channelRefundNo(refund.getChannelRefundNo())
                .refundAmount(refund.getRefundAmount())
                .refundTime(refund.getRefundTime())
                .build();
    }

    @Override
    public IPage<PayRefund> listPage(Long current, Long size, String merchantNo, Map<String, Object> params) {
        Page<PayRefund> page = new Page<>(current, size);
        LambdaQueryWrapper<PayRefund> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PayRefund::getMerchantNo, merchantNo);
        if (params != null) {
            if (params.get("refundNo") != null && StrUtil.isNotBlank(params.get("refundNo").toString())) {
                wrapper.eq(PayRefund::getRefundNo, params.get("refundNo").toString());
            }
            if (params.get("orderNo") != null && StrUtil.isNotBlank(params.get("orderNo").toString())) {
                wrapper.eq(PayRefund::getOrderNo, params.get("orderNo").toString());
            }
            if (params.get("merchantRefundNo") != null && StrUtil.isNotBlank(params.get("merchantRefundNo").toString())) {
                wrapper.eq(PayRefund::getMerchantRefundNo, params.get("merchantRefundNo").toString());
            }
            if (params.get("refundStatus") != null) {
                wrapper.eq(PayRefund::getRefundStatus, params.get("refundStatus"));
            }
            if (params.get("startTime") != null && StrUtil.isNotBlank(params.get("startTime").toString())) {
                wrapper.ge(PayRefund::getCreatedAt, params.get("startTime").toString());
            }
            if (params.get("endTime") != null && StrUtil.isNotBlank(params.get("endTime").toString())) {
                wrapper.le(PayRefund::getCreatedAt, params.get("endTime").toString());
            }
        }
        wrapper.orderByDesc(PayRefund::getCreatedAt);
        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryRefund() {
        LambdaQueryWrapper<PayRefund> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PayRefund::getRefundStatus, RefundStatusEnum.FAIL.getCode(), RefundStatusEnum.PROCESSING.getCode())
                .lt(PayRefund::getNextRetryTime, LocalDateTime.now())
                .or(w -> w.eq(PayRefund::getRefundStatus, RefundStatusEnum.FAIL.getCode())
                        .isNull(PayRefund::getNextRetryTime))
                .last("LIMIT 100");

        List<PayRefund> refundList = this.list(wrapper);
        if (refundList == null || refundList.isEmpty()) {
            return;
        }

        log.info("开始执行退款重试任务, 待处理数量: {}", refundList.size());

        for (PayRefund refund : refundList) {
            try {
                executeSingleRetry(refund);
            } catch (Exception e) {
                log.error("退款重试异常: refundNo={}", refund.getRefundNo(), e);
                int newRetryCount = (refund.getRetryCount() == null ? 0 : refund.getRetryCount()) + 1;
                refund.setRetryCount(newRetryCount);
                calculateNextRetryTime(refund);
                this.updateById(refund);
            }
        }

        log.info("退款重试任务执行完成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryRefund(String refundNo) {
        if (StrUtil.isBlank(refundNo)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "退款单号不能为空");
        }

        LambdaQueryWrapper<PayRefund> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PayRefund::getRefundNo, refundNo)
                .last("LIMIT 1");
        PayRefund refund = this.getOne(wrapper);
        if (refund == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "退款订单不存在");
        }

        if (RefundStatusEnum.SUCCESS.getCode().equals(refund.getRefundStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "退款已成功，无需重试");
        }

        if (refund.getRetryCount() != null && refund.getRetryCount() >= MAX_RETRY_COUNT) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "退款重试次数已达上限");
        }

        log.info("手动触发退款重试: refundNo={}, 当前状态={}", refundNo, refund.getRefundStatus());

        try {
            executeSingleRetry(refund);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("手动退款重试异常: refundNo={}", refundNo, e);
            int newRetryCount = (refund.getRetryCount() == null ? 0 : refund.getRetryCount()) + 1;
            refund.setRetryCount(newRetryCount);
            calculateNextRetryTime(refund);
            this.updateById(refund);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "退款重试失败：" + e.getMessage());
        }
    }

    private void executeSingleRetry(PayRefund refund) {
        if (refund.getRetryCount() != null && refund.getRetryCount() >= MAX_RETRY_COUNT) {
            log.warn("退款重试次数已达上限: refundNo={}, retryCount={}",
                    refund.getRefundNo(), refund.getRetryCount());
            return;
        }

        PayOrder order = payOrderService.getOrderDetail(refund.getOrderNo(), refund.getMerchantNo());
        if (order == null) {
            log.warn("退款订单关联的支付订单不存在: refundNo={}", refund.getRefundNo());
            return;
        }

        MerchantPayConfig merchantConfig = getMerchantPayConfig(order);
        if (merchantConfig == null) {
            log.warn("未找到商户支付配置: refundNo={}", refund.getRefundNo());
            return;
        }

        PayChannelConfig channelConfig = payChannelConfigMapper.selectByChannelCode(merchantConfig.getChannelCode());
        if (channelConfig == null) {
            log.warn("未找到通道配置: refundNo={}", refund.getRefundNo());
            return;
        }

        com.payhub.channel.dto.RefundRequest channelRefundRequest = buildChannelRefundRequest(
                refund, order, channelConfig);

        PayChannelStrategy strategy = payChannelStrategyFactory.getStrategy(merchantConfig.getChannelCode());
        com.payhub.channel.dto.RefundResponse channelResponse = strategy.refund(channelRefundRequest);

        int newRetryCount = (refund.getRetryCount() == null ? 0 : refund.getRetryCount()) + 1;
        refund.setRetryCount(newRetryCount);
        refund.setChannelRefundNo(channelResponse.getChannelRefundNo());

        if (channelResponse.isSuccess()) {
            String refundStatus = channelResponse.getRefundStatus();
            if ("SUCCESS".equalsIgnoreCase(refundStatus)) {
                refund.setRefundStatus(RefundStatusEnum.SUCCESS.getCode());
                refund.setRefundTime(LocalDateTime.now());
                refund.setNextRetryTime(null);
                log.info("退款重试成功: refundNo={}, retryCount={}", refund.getRefundNo(), newRetryCount);
            } else if ("FAIL".equalsIgnoreCase(refundStatus)) {
                refund.setRefundStatus(RefundStatusEnum.FAIL.getCode());
                calculateNextRetryTime(refund);
                log.warn("退款重试失败: refundNo={}, retryCount={}, msg={}",
                        refund.getRefundNo(), newRetryCount, channelResponse.getMsg());
            } else {
                refund.setRefundStatus(RefundStatusEnum.PROCESSING.getCode());
                refund.setNextRetryTime(null);
                log.info("退款重试处理中: refundNo={}, retryCount={}", refund.getRefundNo(), newRetryCount);
            }
        } else {
            refund.setRefundStatus(RefundStatusEnum.FAIL.getCode());
            calculateNextRetryTime(refund);
            log.warn("通道退款重试调用失败: refundNo={}, retryCount={}, code={}, msg={}",
                    refund.getRefundNo(), newRetryCount, channelResponse.getCode(), channelResponse.getMsg());
        }

        this.updateById(refund);
        saveRetryInfoToRedis(refund);
    }

    private MerchantPayConfig getMerchantPayConfig(PayOrder order) {
        LambdaQueryWrapper<MerchantPayConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantPayConfig::getMerchantNo, order.getMerchantNo())
                .eq(MerchantPayConfig::getPayChannel, order.getPayChannel())
                .eq(MerchantPayConfig::getPayType, order.getPayType())
                .eq(MerchantPayConfig::getStatus, 1)
                .last("LIMIT 1");
        return merchantPayConfigService.getOne(wrapper);
    }

    private com.payhub.channel.dto.RefundRequest buildChannelRefundRequest(
            PayRefund refund, PayOrder order, PayChannelConfig channelConfig) {
        com.payhub.channel.dto.RefundRequest request = new com.payhub.channel.dto.RefundRequest();
        request.setOrderNo(order.getOrderNo());
        request.setRefundNo(refund.getRefundNo());
        request.setRefundAmount(refund.getRefundAmount());
        request.setRefundReason(refund.getRefundReason());
        request.setChannelMerchantId(channelConfig.getChannelMerchantId());
        request.setChannelSecretKey(channelConfig.getChannelSecretKey());
        request.setChannelTradeNo(order.getChannelTradeNo());
        return request;
    }

    private void calculateNextRetryTime(PayRefund refund) {
        int retryCount = refund.getRetryCount() == null ? 0 : refund.getRetryCount();
        if (retryCount >= MAX_RETRY_COUNT) {
            refund.setNextRetryTime(null);
            return;
        }
        long delayMinutes = BASE_RETRY_INTERVAL_MINUTES * (1L << retryCount);
        refund.setNextRetryTime(LocalDateTime.now().plusMinutes(delayMinutes));
    }

    private void saveRetryInfoToRedis(PayRefund refund) {
        try {
            String key = REFUND_RETRY_KEY_PREFIX + refund.getRefundNo();
            String value = refund.getRetryCount() + ":" +
                    (refund.getNextRetryTime() != null ? refund.getNextRetryTime().toString() : "");
            stringRedisTemplate.opsForValue().set(key, value, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("保存退款重试信息到Redis失败: refundNo={}", refund.getRefundNo(), e);
        }
    }
}
