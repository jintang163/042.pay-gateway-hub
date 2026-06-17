package com.payhub.pay.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.channel.dto.NotifyResult;
import com.payhub.channel.strategy.PayChannelStrategy;
import com.payhub.channel.strategy.PayChannelStrategyFactory;
import com.payhub.common.enums.PayStatusEnum;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.HttpUtil;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.pay.dto.*;
import com.payhub.pay.entity.MerchantPayConfig;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.mapper.PayOrderMapper;
import com.payhub.pay.service.PayOrderService;
import com.payhub.pay.service.PayRouterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class PayOrderServiceImpl extends ServiceImpl<PayOrderMapper, PayOrder> implements PayOrderService {

    @Autowired
    private PayRouterService payRouterService;

    @Autowired
    private PayChannelStrategyFactory payChannelStrategyFactory;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UnifiedOrderResponse unifiedOrder(UnifiedOrderRequest request) {
        LambdaQueryWrapper<PayOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(PayOrder::getMerchantNo, request.getMerchantNo())
                .eq(PayOrder::getMerchantOrderNo, request.getMerchantOrderNo())
                .last("LIMIT 1");
        PayOrder existOrder = this.getOne(orderWrapper);
        if (existOrder != null && !PayStatusEnum.FAIL.getCode().equals(existOrder.getPayStatus())) {
            return UnifiedOrderResponse.builder()
                    .orderNo(existOrder.getOrderNo())
                    .payType(existOrder.getPayType())
                    .payStatus(existOrder.getPayStatus())
                    .build();
        }

        MerchantPayConfig config = payRouterService.selectChannel(
                request.getMerchantNo(),
                request.getPayChannel(),
                request.getPayType(),
                request.getPayAmount(),
                request.getClientIp()
        );
        if (config == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "未找到可用的支付通道配置");
        }

        String orderNo = OrderNoGenerator.generate();
        BigDecimal feeAmount = calculateFee(request.getPayAmount(), config.getFeeRate(), config.getMinFee(), config.getMaxFee());
        BigDecimal actualAmount = request.getPayAmount().subtract(feeAmount);

        PayOrder order = new PayOrder();
        order.setOrderNo(orderNo);
        order.setMerchantNo(request.getMerchantNo());
        order.setMerchantOrderNo(request.getMerchantOrderNo());
        order.setPayAmount(request.getPayAmount());
        order.setActualAmount(actualAmount);
        order.setFeeAmount(feeAmount);
        order.setPayChannel(request.getPayChannel());
        order.setPayType(request.getPayType());
        order.setUserIdentity(request.getUserIdentity());
        order.setProductSubject(request.getProductSubject());
        order.setProductDetail(request.getProductDetail());
        order.setNotifyUrl(request.getNotifyUrl());
        order.setClientIp(request.getClientIp());
        order.setExtraParams(request.getExtraParams());
        order.setPayStatus(PayStatusEnum.PENDING.getCode());
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));

        this.save(order);

        log.info("支付订单创建成功: orderNo={}, merchantNo={}, amount={}", orderNo, request.getMerchantNo(), request.getPayAmount());

        return UnifiedOrderResponse.builder()
                .orderNo(orderNo)
                .payType(request.getPayType())
                .payParams(buildPayParams(order, config))
                .payStatus(PayStatusEnum.PENDING.getCode())
                .build();
    }

    @Override
    public OrderQueryResponse queryOrder(OrderQueryRequest request) {
        LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(request.getOrderNo())) {
            wrapper.eq(PayOrder::getOrderNo, request.getOrderNo());
        } else if (StrUtil.isNotBlank(request.getMerchantOrderNo())) {
            wrapper.eq(PayOrder::getMerchantNo, request.getMerchantNo())
                    .eq(PayOrder::getMerchantOrderNo, request.getMerchantOrderNo());
        } else {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单号和商户订单号不能同时为空");
        }
        wrapper.last("LIMIT 1");
        PayOrder order = this.getOne(wrapper);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        return OrderQueryResponse.builder()
                .orderNo(order.getOrderNo())
                .merchantOrderNo(order.getMerchantOrderNo())
                .payAmount(order.getPayAmount())
                .actualAmount(order.getActualAmount())
                .feeAmount(order.getFeeAmount())
                .payStatus(order.getPayStatus())
                .payTime(order.getPayTime())
                .channelTradeNo(order.getChannelTradeNo())
                .build();
    }

    private BigDecimal calculateFee(BigDecimal amount, BigDecimal feeRate, BigDecimal minFee, BigDecimal maxFee) {
        if (feeRate == null) {
            feeRate = BigDecimal.ZERO;
        }
        BigDecimal fee = amount.multiply(feeRate).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
        if (minFee != null && fee.compareTo(minFee) < 0) {
            fee = minFee;
        }
        if (maxFee != null && fee.compareTo(maxFee) > 0) {
            fee = maxFee;
        }
        return fee;
    }

    private String buildPayParams(PayOrder order, MerchantPayConfig config) {
        return JSON.toJSONString(BeanUtil.beanToMap(order));
    }

    @Override
    public PayOrder getOrderDetail(String orderNo, String merchantNo) {
        LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PayOrder::getOrderNo, orderNo)
                .eq(PayOrder::getMerchantNo, merchantNo)
                .last("LIMIT 1");
        PayOrder order = this.getOne(wrapper);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    @Override
    public IPage<PayOrder> listPage(Long current, Long size, String merchantNo, Map<String, Object> params) {
        Page<PayOrder> page = new Page<>(current, size);
        LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PayOrder::getMerchantNo, merchantNo);
        if (params != null) {
            if (params.get("orderNo") != null && StrUtil.isNotBlank(params.get("orderNo").toString())) {
                wrapper.eq(PayOrder::getOrderNo, params.get("orderNo").toString());
            }
            if (params.get("merchantOrderNo") != null && StrUtil.isNotBlank(params.get("merchantOrderNo").toString())) {
                wrapper.eq(PayOrder::getMerchantOrderNo, params.get("merchantOrderNo").toString());
            }
            if (params.get("payStatus") != null) {
                wrapper.eq(PayOrder::getPayStatus, params.get("payStatus"));
            }
            if (params.get("payChannel") != null && StrUtil.isNotBlank(params.get("payChannel").toString())) {
                wrapper.eq(PayOrder::getPayChannel, params.get("payChannel").toString());
            }
            if (params.get("startTime") != null && StrUtil.isNotBlank(params.get("startTime").toString())) {
                wrapper.ge(PayOrder::getCreatedAt, params.get("startTime").toString());
            }
            if (params.get("endTime") != null && StrUtil.isNotBlank(params.get("endTime").toString())) {
                wrapper.le(PayOrder::getCreatedAt, params.get("endTime").toString());
            }
        }
        wrapper.orderByDesc(PayOrder::getCreatedAt);
        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleNotify(String channel, Map<String, String> params, String body) {
        log.info("接收支付异步通知, channel={}, params={}, body={}", channel, params, body);

        PayChannelStrategy strategy;
        try {
            strategy = payChannelStrategyFactory.getStrategy(channel);
        } catch (Exception e) {
            log.warn("不支持的支付通道: {}", channel);
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的支付通道");
        }

        if (!strategy.verifyNotify(params)) {
            log.warn("异步通知签名验证失败, channel={}, params={}", channel, params);
            throw new BusinessException(ResultCode.SIGN_ERROR);
        }

        NotifyResult notifyResult = strategy.parseNotify(body, params);
        if (notifyResult == null || StrUtil.isBlank(notifyResult.getOrderNo())) {
            log.warn("解析异步通知失败, channel={}, body={}", channel, body);
            throw new BusinessException(ResultCode.PARAM_ERROR, "解析通知数据失败");
        }

        LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PayOrder::getOrderNo, notifyResult.getOrderNo())
                .last("LIMIT 1");
        PayOrder order = this.getOne(wrapper);
        if (order == null) {
            log.warn("订单不存在, orderNo={}", notifyResult.getOrderNo());
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        if (!PayStatusEnum.PENDING.getCode().equals(order.getPayStatus())
                && !PayStatusEnum.FAIL.getCode().equals(order.getPayStatus())) {
            log.info("订单状态已处理, orderNo={}, status={}", order.getOrderNo(), order.getPayStatus());
            return "success";
        }

        if ("SUCCESS".equalsIgnoreCase(notifyResult.getPayStatus())
                || PayStatusEnum.SUCCESS.getCode().toString().equals(notifyResult.getPayStatus())) {
            order.setPayStatus(PayStatusEnum.SUCCESS.getCode());
            order.setPayTime(notifyResult.getPayTime() != null ? notifyResult.getPayTime() : LocalDateTime.now());
            order.setChannelTradeNo(notifyResult.getChannelTradeNo());
            this.updateById(order);
            log.info("订单支付成功, orderNo={}, channelTradeNo={}", order.getOrderNo(), notifyResult.getChannelTradeNo());

            notifyMerchant(order);
        } else if ("FAIL".equalsIgnoreCase(notifyResult.getPayStatus())
                || PayStatusEnum.FAIL.getCode().toString().equals(notifyResult.getPayStatus())) {
            order.setPayStatus(PayStatusEnum.FAIL.getCode());
            this.updateById(order);
            log.info("订单支付失败, orderNo={}", order.getOrderNo());

            notifyMerchant(order);
        }

        return "success";
    }

    private void notifyMerchant(PayOrder order) {
        if (StrUtil.isBlank(order.getNotifyUrl())) {
            return;
        }
        try {
            Map<String, Object> notifyParams = BeanUtil.beanToMap(order);
            String response = HttpUtil.postJson(order.getNotifyUrl(), notifyParams);
            log.info("通知商户结果, orderNo={}, response={}", order.getOrderNo(), response);
        } catch (Exception e) {
            log.error("通知商户失败, orderNo={}, notifyUrl={}", order.getOrderNo(), order.getNotifyUrl(), e);
        }
    }
}
