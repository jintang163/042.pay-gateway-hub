package com.payhub.pay.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payhub.channel.dto.UnifiedOrderResponse;
import com.payhub.channel.strategy.PayChannelStrategy;
import com.payhub.channel.strategy.PayChannelStrategyFactory;
import com.payhub.common.enums.PayStatusEnum;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.common.utils.QrCodeUtil;
import com.payhub.pay.dto.AggregateCodeOrderRequest;
import com.payhub.pay.dto.AggregateCodeOrderResponse;
import com.payhub.pay.dto.AggregateCodeQueryResponse;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.service.AggregateCodePayService;
import com.payhub.pay.service.PayOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class AggregateCodePayServiceImpl implements AggregateCodePayService {

    public static final String CHANNEL_ALIPAY = "ALIPAY";
    public static final String CHANNEL_WECHAT = "WECHAT_PAY";
    public static final String CHANNEL_UNIONPAY = "UNION_PAY";
    public static final String CHANNEL_AGGREGATE = "AGGREGATE";

    public static final String TYPE_NATIVE = "NATIVE";
    public static final String TYPE_JSAPI = "JSAPI";

    @Value("${payhub.aggregate.qr-code.base-url:/api/public/aggregate/qr}")
    private String qrCodeBaseUrl;

    @Value("${payhub.aggregate.qr-code.default-size:300}")
    private int defaultQrCodeSize;

    @Value("${payhub.aggregate.order-expire-minutes:30}")
    private int orderExpireMinutes;

    @Autowired
    private PayOrderService payOrderService;

    @Autowired
    private PayChannelStrategyFactory payChannelStrategyFactory;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AggregateCodeOrderResponse createOrder(AggregateCodeOrderRequest request) {
        log.info("创建聚合码支付订单: merchantNo={}, merchantOrderNo={}, amount={}",
                request.getMerchantNo(), request.getMerchantOrderNo(), request.getPayAmount());

        LambdaQueryWrapper<PayOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(PayOrder::getMerchantNo, request.getMerchantNo())
                .eq(PayOrder::getMerchantOrderNo, request.getMerchantOrderNo())
                .last("LIMIT 1");
        PayOrder existOrder = payOrderService.getOne(orderWrapper);
        if (existOrder != null && !PayStatusEnum.FAIL.getCode().equals(existOrder.getPayStatus())) {
            String qrCodeUrl = buildQrCodeUrl(existOrder.getOrderNo());
            String qrCodeBase64 = QrCodeUtil.generateBase64(qrCodeUrl, defaultQrCodeSize);
            return AggregateCodeOrderResponse.builder()
                    .orderNo(existOrder.getOrderNo())
                    .merchantOrderNo(existOrder.getMerchantOrderNo())
                    .payAmount(existOrder.getPayAmount())
                    .qrCodeUrl(qrCodeUrl)
                    .qrCodeBase64(qrCodeBase64)
                    .qrCodeSize(defaultQrCodeSize)
                    .payStatus(existOrder.getPayStatus())
                    .expireTime(existOrder.getExpireTime())
                    .payChannel(CHANNEL_AGGREGATE)
                    .codeUrl(qrCodeUrl)
                    .build();
        }

        String orderNo = OrderNoGenerator.generate();

        PayOrder order = new PayOrder();
        order.setOrderNo(orderNo);
        order.setMerchantNo(request.getMerchantNo());
        order.setMerchantOrderNo(request.getMerchantOrderNo());
        order.setPayAmount(request.getPayAmount());
        order.setPayChannel(CHANNEL_AGGREGATE);
        order.setPayType(TYPE_NATIVE);
        order.setProductSubject(request.getProductSubject());
        order.setProductDetail(request.getProductDetail());
        order.setNotifyUrl(request.getNotifyUrl());
        order.setClientIp(request.getClientIp());
        order.setExtraParams(request.getExtraParams());
        order.setPayStatus(PayStatusEnum.PENDING.getCode());
        order.setExpireTime(LocalDateTime.now().plusMinutes(orderExpireMinutes));
        order.setFeeAmount(BigDecimal.ZERO);
        order.setActualAmount(request.getPayAmount());

        payOrderService.save(order);

        String qrCodeUrl = buildQrCodeUrl(orderNo);
        String qrCodeBase64 = QrCodeUtil.generateBase64(qrCodeUrl, defaultQrCodeSize);

        log.info("聚合码订单创建成功: orderNo={}, qrCodeUrl={}", orderNo, qrCodeUrl);

        return AggregateCodeOrderResponse.builder()
                .orderNo(orderNo)
                .merchantOrderNo(request.getMerchantOrderNo())
                .payAmount(request.getPayAmount())
                .qrCodeUrl(qrCodeUrl)
                .qrCodeBase64(qrCodeBase64)
                .qrCodeSize(defaultQrCodeSize)
                .payStatus(PayStatusEnum.PENDING.getCode())
                .expireTime(order.getExpireTime())
                .payChannel(CHANNEL_AGGREGATE)
                .codeUrl(qrCodeUrl)
                .build();
    }

    @Override
    public AggregateCodeQueryResponse queryOrder(String orderNo, String merchantNo) {
        PayOrder order = payOrderService.getOrderDetail(orderNo, merchantNo);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        String statusDesc;
        Integer status = order.getPayStatus();
        if (status == null) {
            statusDesc = "待支付";
        } else {
            switch (status) {
                case 0:
                    statusDesc = "待支付";
                    break;
                case 1:
                    statusDesc = "支付成功";
                    break;
                case 2:
                    statusDesc = "支付失败";
                    break;
                case 3:
                    statusDesc = "已关闭";
                    break;
                default:
                    statusDesc = "未知";
            }
        }

        return AggregateCodeQueryResponse.builder()
                .orderNo(order.getOrderNo())
                .merchantOrderNo(order.getMerchantOrderNo())
                .payChannel(order.getPayChannel())
                .payType(order.getPayType())
                .payAmount(order.getPayAmount())
                .payStatus(order.getPayStatus())
                .payStatusDesc(statusDesc)
                .channelTradeNo(order.getChannelTradeNo())
                .payTime(order.getPayTime())
                .createTime(order.getCreatedAt())
                .expireTime(order.getExpireTime())
                .build();
    }

    @Override
    public String recognizeChannel(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (StrUtil.isBlank(userAgent)) {
            return null;
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("micromessenger")) {
            return CHANNEL_WECHAT;
        }
        if (ua.contains("alipay") || ua.contains("alipayclient")) {
            return CHANNEL_ALIPAY;
        }
        if (ua.contains("unionpay") || ua.contains("cloudpay") || ua.contains("uppay")
                || ua.contains("yunshanfu") || ua.contains("yun.shan.fu")) {
            return CHANNEL_UNIONPAY;
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AggregateCodeOrderResponse getOrCreateChannelOrder(String orderNo, String channel, String payType, HttpServletRequest request) {
        PayOrder order = payOrderService.getOne(
                new LambdaQueryWrapper<PayOrder>().eq(PayOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        if (StrUtil.isBlank(channel)) {
            channel = recognizeChannel(request);
        }
        if (StrUtil.isBlank(channel)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无法识别支付渠道，请使用微信/支付宝/云闪付APP扫码");
        }

        String targetPayType = StrUtil.isNotBlank(payType) ? payType : resolvePayTypeByChannel(channel, request);

        if (order.getPayStatus() != null && order.getPayStatus() == 1) {
            log.info("订单已支付成功，直接返回: orderNo={}", orderNo);
            String qrCodeUrl = buildQrCodeUrl(orderNo);
            return AggregateCodeOrderResponse.builder()
                    .orderNo(orderNo)
                    .merchantOrderNo(order.getMerchantOrderNo())
                    .payAmount(order.getPayAmount())
                    .qrCodeUrl(qrCodeUrl)
                    .qrCodeSize(defaultQrCodeSize)
                    .payStatus(order.getPayStatus())
                    .expireTime(order.getExpireTime())
                    .payChannel(order.getPayChannel())
                    .codeUrl(qrCodeUrl)
                    .build();
        }

        if (!CHANNEL_AGGREGATE.equals(order.getPayChannel())
                && channel.equals(order.getPayChannel())
                && StrUtil.isNotBlank(order.getChannelTradeNo())) {
            log.info("订单已在指定渠道创建，直接返回: orderNo={}, channel={}", orderNo, channel);
            return buildChannelOrderResponse(order);
        }

        UnifiedOrderResponse channelResponse;
        try {
            com.payhub.channel.dto.UnifiedOrderRequest channelRequest = buildChannelRequest(order, channel, targetPayType, request);
            PayChannelStrategy strategy = payChannelStrategyFactory.getStrategy(channel);
            channelResponse = strategy.unifiedOrder(channelRequest);
        } catch (Exception e) {
            log.error("创建渠道订单失败: orderNo={}, channel={}", orderNo, channel, e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "创建渠道支付订单失败: " + e.getMessage());
        }

        if (channelResponse == null || !channelResponse.isSuccess()) {
            String msg = channelResponse != null ? channelResponse.getMsg() : "渠道下单失败";
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "渠道下单失败: " + msg);
        }

        order.setPayChannel(channel);
        order.setPayType(targetPayType);
        order.setChannelTradeNo(channelResponse.getChannelTradeNo());
        payOrderService.updateById(order);

        String qrCodeUrl = buildQrCodeUrl(orderNo);

        AggregateCodeOrderResponse resp = AggregateCodeOrderResponse.builder()
                .orderNo(orderNo)
                .merchantOrderNo(order.getMerchantOrderNo())
                .payAmount(order.getPayAmount())
                .qrCodeUrl(qrCodeUrl)
                .qrCodeSize(defaultQrCodeSize)
                .payStatus(order.getPayStatus())
                .expireTime(order.getExpireTime())
                .payChannel(channel)
                .payParams(channelResponse.getPayParams())
                .codeUrl(qrCodeUrl)
                .build();

        log.info("聚合码渠道订单创建成功: orderNo={}, channel={}, payType={}, channelTradeNo={}",
                orderNo, channel, targetPayType, channelResponse.getChannelTradeNo());

        return resp;
    }

    private AggregateCodeOrderResponse buildChannelOrderResponse(PayOrder order) {
        String qrCodeUrl = buildQrCodeUrl(order.getOrderNo());
        return AggregateCodeOrderResponse.builder()
                .orderNo(order.getOrderNo())
                .merchantOrderNo(order.getMerchantOrderNo())
                .payAmount(order.getPayAmount())
                .qrCodeUrl(qrCodeUrl)
                .qrCodeSize(defaultQrCodeSize)
                .payStatus(order.getPayStatus())
                .expireTime(order.getExpireTime())
                .payChannel(order.getPayChannel())
                .codeUrl(qrCodeUrl)
                .build();
    }

    private com.payhub.channel.dto.UnifiedOrderRequest buildChannelRequest(
            PayOrder order, String channel, String payType, HttpServletRequest request) {
        com.payhub.channel.dto.UnifiedOrderRequest channelRequest = new com.payhub.channel.dto.UnifiedOrderRequest();
        channelRequest.setMerchantNo(order.getMerchantNo());
        channelRequest.setOrderNo(order.getOrderNo());
        channelRequest.setAmount(order.getPayAmount());
        channelRequest.setSubject(order.getProductSubject());
        channelRequest.setDetail(order.getProductDetail());
        channelRequest.setUserIdentity(order.getUserIdentity());
        channelRequest.setClientIp(order.getClientIp());
        channelRequest.setPayType(payType);
        channelRequest.setNotifyUrl("/api/pay/notify/" + channel.toLowerCase());
        return channelRequest;
    }

    @Override
    public String getQrCodeUrl(String orderNo) {
        return buildQrCodeUrl(orderNo);
    }

    @Override
    public String generateQrCodeBase64(String orderNo, int size) {
        String url = buildQrCodeUrl(orderNo);
        return QrCodeUtil.generateBase64(url, size > 0 ? size : defaultQrCodeSize);
    }

    private String buildQrCodeUrl(String orderNo) {
        String base = qrCodeBaseUrl;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + orderNo;
    }

    private String resolvePayTypeByChannel(String channel, HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (StrUtil.isNotBlank(ua)) {
            String lowerUa = ua.toLowerCase();
            if (CHANNEL_WECHAT.equals(channel) && lowerUa.contains("micromessenger")) {
                return TYPE_JSAPI;
            }
            if (CHANNEL_ALIPAY.equals(channel) && (lowerUa.contains("alipay") || lowerUa.contains("alipayclient"))) {
                return TYPE_JSAPI;
            }
        }
        return TYPE_NATIVE;
    }
}
