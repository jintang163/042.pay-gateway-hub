package com.payhub.pay.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
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
import com.payhub.pay.entity.MerchantPayConfig;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.service.AggregateCodePayService;
import com.payhub.pay.service.PayOrderService;
import com.payhub.pay.service.PayRouterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
public class AggregateCodePayServiceImpl implements AggregateCodePayService {

    public static final String CHANNEL_ALIPAY = "ALIPAY";
    public static final String CHANNEL_WECHAT = "WECHAT_PAY";
    public static final String CHANNEL_UNIONPAY = "UNION_PAY";
    public static final String CHANNEL_AGGREGATE = "AGGREGATE";

    public static final String TYPE_NATIVE = "NATIVE";
    public static final String TYPE_JSAPI = "JSAPI";

    @Value("${payhub.aggregate.gateway-domain:http://localhost:8080}")
    private String gatewayDomain;

    @Value("${payhub.aggregate.qr-code.path:/api/public/aggregate/qr}")
    private String qrCodePath;

    @Value("${payhub.aggregate.qr-code.default-size:300}")
    private int defaultQrCodeSize;

    @Value("${payhub.aggregate.order-expire-minutes:30}")
    private int orderExpireMinutes;

    @Value("${payhub.aggregate.notify-path:/api/pay/notify}")
    private String notifyPath;

    @Autowired
    private PayOrderService payOrderService;

    @Autowired
    private PayChannelStrategyFactory payChannelStrategyFactory;

    @Autowired
    private PayRouterService payRouterService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AggregateCodeOrderResponse createOrder(AggregateCodeOrderRequest request) {
        log.info("创建聚合码支付订单: merchantNo={}, merchantOrderNo={}, amount={}",
                request.getMerchantNo(), request.getMerchantOrderNo(), request.getPayAmount());

        LambdaQueryWrapper<PayOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(PayOrder::getMerchantNo, request.getMerchantNo())
                .eq(PayOrder::getMerchantOrderNo, request.getMerchantOrderNo())
                .isNull(PayOrder::getParentOrderNo)
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
        order.setParentOrderNo(null);

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
                .merchantNo(order.getMerchantNo())
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
        PayOrder aggregateOrder = payOrderService.getOne(
                new LambdaQueryWrapper<PayOrder>().eq(PayOrder::getOrderNo, orderNo));
        if (aggregateOrder == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        if (aggregateOrder.getPayStatus() != null && PayStatusEnum.SUCCESS.getCode().equals(aggregateOrder.getPayStatus())) {
            log.info("聚合单已支付成功，直接返回: orderNo={}", orderNo);
            String qrCodeUrl = buildQrCodeUrl(orderNo);
            return AggregateCodeOrderResponse.builder()
                    .orderNo(orderNo)
                    .merchantOrderNo(aggregateOrder.getMerchantOrderNo())
                    .payAmount(aggregateOrder.getPayAmount())
                    .qrCodeUrl(qrCodeUrl)
                    .qrCodeSize(defaultQrCodeSize)
                    .payStatus(aggregateOrder.getPayStatus())
                    .expireTime(aggregateOrder.getExpireTime())
                    .payChannel(aggregateOrder.getPayChannel())
                    .codeUrl(qrCodeUrl)
                    .build();
        }

        if (StrUtil.isBlank(channel)) {
            channel = recognizeChannel(request);
        }
        if (StrUtil.isBlank(channel)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无法识别支付渠道，请使用微信/支付宝/云闪付APP扫码");
        }

        String targetPayType = StrUtil.isNotBlank(payType) ? payType : resolvePayTypeByChannel(channel, request);

        LambdaQueryWrapper<PayOrder> subOrderWrapper = new LambdaQueryWrapper<>();
        subOrderWrapper.eq(PayOrder::getParentOrderNo, orderNo)
                .eq(PayOrder::getPayChannel, channel)
                .last("LIMIT 1");
        PayOrder existSubOrder = payOrderService.getOne(subOrderWrapper);
        if (existSubOrder != null && StrUtil.isNotBlank(existSubOrder.getChannelTradeNo())) {
            log.info("该渠道子单已存在，直接返回: orderNo={}, channel={}, subOrderNo={}",
                    orderNo, channel, existSubOrder.getOrderNo());
            return buildSubOrderResponse(aggregateOrder, existSubOrder);
        }

        MerchantPayConfig config = payRouterService.selectChannel(
                aggregateOrder.getMerchantNo(),
                channel,
                targetPayType,
                aggregateOrder.getPayAmount(),
                aggregateOrder.getClientIp()
        );
        if (config == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "未找到可用的支付通道配置，渠道=" + channel);
        }

        String subOrderNo = OrderNoGenerator.generate();
        PayOrder subOrder = new PayOrder();
        subOrder.setOrderNo(subOrderNo);
        subOrder.setParentOrderNo(orderNo);
        subOrder.setMerchantNo(aggregateOrder.getMerchantNo());
        subOrder.setMerchantOrderNo(aggregateOrder.getMerchantOrderNo());
        subOrder.setPayAmount(aggregateOrder.getPayAmount());
        subOrder.setPayChannel(config.getChannelCode());
        subOrder.setPayType(targetPayType);
        subOrder.setProductSubject(aggregateOrder.getProductSubject());
        subOrder.setProductDetail(aggregateOrder.getProductDetail());
        subOrder.setNotifyUrl(aggregateOrder.getNotifyUrl());
        subOrder.setClientIp(aggregateOrder.getClientIp());
        subOrder.setExtraParams(aggregateOrder.getExtraParams());
        subOrder.setPayStatus(PayStatusEnum.PENDING.getCode());
        subOrder.setExpireTime(aggregateOrder.getExpireTime());
        subOrder.setFeeAmount(BigDecimal.ZERO);
        subOrder.setActualAmount(aggregateOrder.getPayAmount());
        payOrderService.save(subOrder);

        UnifiedOrderResponse channelResponse;
        try {
            com.payhub.channel.dto.UnifiedOrderRequest channelRequest = buildChannelRequest(subOrder, config);
            PayChannelStrategy strategy = payChannelStrategyFactory.getStrategy(config.getChannelCode());
            channelResponse = strategy.unifiedOrder(channelRequest);

            if (strategy instanceof com.payhub.channel.strategy.AbstractPayChannel) {
                ((com.payhub.channel.strategy.AbstractPayChannel) strategy).saveChannelLog(
                        aggregateOrder.getMerchantNo(),
                        subOrderNo,
                        "unifiedOrder",
                        "",
                        JSON.toJSONString(channelRequest),
                        channelResponse != null ? JSON.toJSONString(channelResponse) : "",
                        channelResponse != null ? channelResponse.getChannelTradeNo() : "",
                        0,
                        channelResponse != null && !channelResponse.isSuccess() ? channelResponse.getMsg() : ""
                );
            }
        } catch (Exception e) {
            log.error("创建渠道子单失败: orderNo={}, channel={}, subOrderNo={}", orderNo, channel, subOrderNo, e);
            subOrder.setPayStatus(PayStatusEnum.FAIL.getCode());
            payOrderService.updateById(subOrder);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "创建渠道支付订单失败: " + e.getMessage());
        }

        if (channelResponse == null || !channelResponse.isSuccess()) {
            String msg = channelResponse != null ? channelResponse.getMsg() : "渠道下单失败";
            subOrder.setPayStatus(PayStatusEnum.FAIL.getCode());
            payOrderService.updateById(subOrder);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "渠道下单失败: " + msg);
        }

        subOrder.setChannelTradeNo(channelResponse.getChannelTradeNo());
        payOrderService.updateById(subOrder);

        log.info("聚合码渠道子单创建成功: aggregateOrderNo={}, channel={}, payType={}, subOrderNo={}, channelTradeNo={}",
                orderNo, channel, targetPayType, subOrderNo, channelResponse.getChannelTradeNo());

        String qrCodeUrl = buildQrCodeUrl(orderNo);
        String finalPayParams = buildFinalPayParams(
                channelResponse.getPayType(),
                channelResponse.getPayParams(),
                subOrder);

        return AggregateCodeOrderResponse.builder()
                .orderNo(orderNo)
                .channelOrderNo(subOrderNo)
                .merchantOrderNo(aggregateOrder.getMerchantOrderNo())
                .payAmount(aggregateOrder.getPayAmount())
                .qrCodeUrl(qrCodeUrl)
                .qrCodeSize(defaultQrCodeSize)
                .payStatus(subOrder.getPayStatus())
                .expireTime(aggregateOrder.getExpireTime())
                .payChannel(channel)
                .channelCode(config.getChannelCode())
                .payParams(finalPayParams)
                .codeUrl(qrCodeUrl)
                .build();
    }

    private AggregateCodeOrderResponse buildSubOrderResponse(PayOrder aggregateOrder, PayOrder subOrder) {
        String qrCodeUrl = buildQrCodeUrl(aggregateOrder.getOrderNo());
        return AggregateCodeOrderResponse.builder()
                .orderNo(aggregateOrder.getOrderNo())
                .channelOrderNo(subOrder.getOrderNo())
                .merchantOrderNo(aggregateOrder.getMerchantOrderNo())
                .payAmount(aggregateOrder.getPayAmount())
                .qrCodeUrl(qrCodeUrl)
                .qrCodeSize(defaultQrCodeSize)
                .payStatus(subOrder.getPayStatus())
                .expireTime(aggregateOrder.getExpireTime())
                .payChannel(subOrder.getPayChannel())
                .channelCode(subOrder.getPayChannel())
                .codeUrl(qrCodeUrl)
                .build();
    }

    private String buildFinalPayParams(String payType, String channelPayParams, PayOrder order) {
        if (StrUtil.isBlank(channelPayParams)) {
            return channelPayParams;
        }
        try {
            Map<String, Object> params = JSON.parseObject(channelPayParams, Map.class);
            if (params != null && params.get("expireTime") == null && order.getExpireTime() != null) {
                params.put("expireTime", order.getExpireTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            return JSON.toJSONString(params);
        } catch (Exception e) {
            return channelPayParams;
        }
    }

    private com.payhub.channel.dto.UnifiedOrderRequest buildChannelRequest(
            PayOrder subOrder, MerchantPayConfig config) {
        com.payhub.channel.dto.UnifiedOrderRequest channelRequest = new com.payhub.channel.dto.UnifiedOrderRequest();
        channelRequest.setMerchantNo(subOrder.getMerchantNo());
        channelRequest.setOrderNo(subOrder.getOrderNo());
        channelRequest.setAmount(subOrder.getPayAmount());
        channelRequest.setSubject(subOrder.getProductSubject());
        channelRequest.setDetail(subOrder.getProductDetail());
        channelRequest.setUserIdentity(subOrder.getUserIdentity());
        channelRequest.setClientIp(subOrder.getClientIp());
        channelRequest.setPayType(subOrder.getPayType());
        channelRequest.setNotifyUrl(buildFullNotifyUrl(config.getChannelCode()));
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
        String domain = normalizeDomain(gatewayDomain);
        String path = qrCodePath.startsWith("/") ? qrCodePath : "/" + qrCodePath;
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return domain + path + "/" + orderNo;
    }

    private String buildFullNotifyUrl(String channelCode) {
        String domain = normalizeDomain(gatewayDomain);
        String path = notifyPath.startsWith("/") ? notifyPath : "/" + notifyPath;
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return domain + path + "/" + (channelCode != null ? channelCode.toLowerCase() : "");
    }

    private String normalizeDomain(String domain) {
        if (StrUtil.isBlank(domain)) {
            return "http://localhost:8080";
        }
        String d = domain.trim();
        if (!d.startsWith("http://") && !d.startsWith("https://")) {
            d = "https://" + d;
        }
        if (d.endsWith("/")) {
            d = d.substring(0, d.length() - 1);
        }
        return d;
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
