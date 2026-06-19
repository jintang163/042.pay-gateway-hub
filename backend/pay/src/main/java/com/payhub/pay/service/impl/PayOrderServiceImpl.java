package com.payhub.pay.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.channel.dto.NotifyResult;
import com.payhub.channel.dto.UnifiedOrderResponse;
import com.payhub.channel.strategy.PayChannelStrategy;
import com.payhub.channel.strategy.PayChannelStrategyFactory;
import com.payhub.common.enums.PayStatusEnum;
import com.payhub.common.context.SandboxContext;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.HttpUtil;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.common.utils.SignUtil;
import com.payhub.common.utils.Sm4Util;
import com.payhub.merchant.entity.MerchantInfo;
import com.payhub.merchant.mapper.MerchantInfoMapper;
import com.payhub.merchant.service.MerchantInfoService;
import com.payhub.merchant.service.FeeRuleService;
import com.payhub.merchant.dto.FeeCalcRequest;
import com.payhub.merchant.dto.FeeCalcResult;
import com.payhub.pay.dto.*;
import com.payhub.pay.entity.MerchantPayConfig;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.mapper.PayOrderMapper;
import com.payhub.pay.service.PayOrderService;
import com.payhub.pay.service.PayRouterService;
import com.payhub.risk.dto.RiskCheckRequest;
import com.payhub.risk.dto.RiskCheckResult;
import com.payhub.risk.service.RiskControlService;
import com.payhub.marketing.service.MarketingDiscountService;
import com.payhub.marketing.dto.CouponDiscountCalcResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.settlement.service.AgentProfitService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PayOrderServiceImpl extends ServiceImpl<PayOrderMapper, PayOrder> implements PayOrderService {

    private static final String DEFAULT_MERCHANT_SECRET = "payhub_default_secret_key_2024";
    private static final String DEFAULT_SIGN_TYPE = "MD5";

    @Autowired
    private PayRouterService payRouterService;

    @Autowired
    private PayChannelStrategyFactory payChannelStrategyFactory;

    @Autowired
    private MerchantInfoService merchantInfoService;

    @Autowired
    private MerchantInfoMapper merchantInfoMapper;

    @Autowired(required = false)
    private FeeRuleService feeRuleService;

    @Autowired(required = false)
    @Lazy
    private AgentProfitService agentProfitService;

    @Autowired(required = false)
    private RiskControlService riskControlService;

    @Autowired(required = false)
    private MarketingDiscountService marketingDiscountService;

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

        BigDecimal couponDiscount = BigDecimal.ZERO;
        BigDecimal activityDiscount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (marketingDiscountService != null) {
            if (StrUtil.isNotBlank(request.getCouponCode())) {
                try {
                    CouponDiscountCalcResult couponResult = marketingDiscountService.calcCouponDiscount(
                            request.getCouponCode(), request.getMerchantNo(), request.getPayAmount());
                    couponDiscount = couponResult.getDiscountAmount();
                    log.info("优惠券抵扣计算成功: couponCode={}, orderAmount={}, discountAmount={}",
                            request.getCouponCode(), request.getPayAmount(), couponDiscount);
                } catch (Exception e) {
                    log.warn("优惠券抵扣计算失败，忽略优惠券: couponCode={}, error={}", request.getCouponCode(), e.getMessage());
                    throw new BusinessException(ResultCode.PARAM_ERROR, "优惠券不可用: " + e.getMessage());
                }
            }
            if (StrUtil.isNotBlank(request.getActivityCode())) {
                try {
                    activityDiscount = marketingDiscountService.calcActivityDiscount(
                            request.getActivityCode(), request.getMerchantNo(), request.getPayAmount());
                    log.info("活动优惠计算成功: activityCode={}, orderAmount={}, discountAmount={}",
                            request.getActivityCode(), request.getPayAmount(), activityDiscount);
                } catch (Exception e) {
                    log.warn("活动优惠计算失败，忽略活动: activityCode={}, error={}", request.getActivityCode(), e.getMessage());
                    throw new BusinessException(ResultCode.PARAM_ERROR, "活动不可用: " + e.getMessage());
                }
            }
        }

        discountAmount = couponDiscount.add(activityDiscount);
        if (discountAmount.compareTo(request.getPayAmount()) > 0) {
            discountAmount = request.getPayAmount();
        }
        BigDecimal payableAmount = request.getPayAmount().subtract(discountAmount);
        if (payableAmount.compareTo(BigDecimal.ZERO) < 0) {
            payableAmount = BigDecimal.ZERO;
        }

        BigDecimal feeAmount;
        if (feeRuleService != null) {
            try {
                FeeCalcRequest calcRequest = new FeeCalcRequest();
                calcRequest.setMerchantNo(request.getMerchantNo());
                calcRequest.setPayChannel(config.getPayChannel());
                calcRequest.setAmount(payableAmount);
                FeeCalcResult calcResult = feeRuleService.calculate(calcRequest);
                feeAmount = calcResult.getFeeAmount();
                log.info("动态费率计算完成: merchantNo={}, amount={}, channel={}, feeAmount={}, ruleNo={}",
                        request.getMerchantNo(), payableAmount, config.getPayChannel(),
                        feeAmount, calcResult.getRuleNo());
            } catch (Exception e) {
                log.warn("动态费率计算失败，回退到配置固定费率: merchantNo={}, error={}",
                        request.getMerchantNo(), e.getMessage());
                feeAmount = calculateFee(payableAmount, config.getFeeRate(), config.getMinFee(), config.getMaxFee());
            }
        } else {
            feeAmount = calculateFee(payableAmount, config.getFeeRate(), config.getMinFee(), config.getMaxFee());
        }

        BigDecimal actualAmount = payableAmount.subtract(feeAmount);

        PayOrder order = new PayOrder();
        order.setOrderNo(orderNo);
        order.setMerchantNo(request.getMerchantNo());
        order.setMerchantOrderNo(request.getMerchantOrderNo());
        order.setLinkCode(request.getLinkCode());
        order.setCouponCode(request.getCouponCode());
        order.setActivityCode(request.getActivityCode());
        order.setPayAmount(request.getPayAmount());
        order.setCouponDiscount(couponDiscount);
        order.setActivityDiscount(activityDiscount);
        order.setActualAmount(actualAmount);
        order.setFeeAmount(feeAmount);
        order.setPayChannel(config.getChannelCode());
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

        log.info("支付订单创建成功: orderNo={}, merchantNo={}, amount={}, channel={}", 
                orderNo, request.getMerchantNo(), request.getPayAmount(), config.getChannelCode());

        if (riskControlService != null) {
            try {
                RiskCheckRequest riskRequest = RiskCheckRequest.builder()
                        .merchantNo(request.getMerchantNo())
                        .orderNo(orderNo)
                        .clientIp(request.getClientIp())
                        .userIdentity(request.getUserIdentity())
                        .payAmount(request.getPayAmount())
                        .payChannel(config.getPayChannel())
                        .payType(request.getPayType())
                        .build();
                RiskCheckResult riskResult = riskControlService.checkRisk(riskRequest);
                if (riskResult != null && !riskResult.isPass()) {
                    String riskMsg = riskResult.getSuggestion() != null
                            ? riskResult.getSuggestion()
                            : "风控拦截: " + (riskResult.getRiskDesc() != null ? riskResult.getRiskDesc() : "高风险交易");
                    log.warn("风控拦截，拒绝下单: orderNo={}, riskLevel={}, riskRules={}, suggestion={}",
                            orderNo, riskResult.getRiskLevel(), riskResult.getRiskRules(), riskResult.getSuggestion());
                    order.setPayStatus(PayStatusEnum.FAIL.getCode());
                    this.updateById(order);
                    throw new BusinessException(ResultCode.RISK_BLOCKED, riskMsg);
                }
                log.info("风控检查通过: orderNo={}, riskLevel={}", orderNo,
                        riskResult != null ? riskResult.getRiskLevel() : 0);
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("风控检查异常，默认放行: orderNo={}, error={}", orderNo, e.getMessage(), e);
            }
        }

        com.payhub.channel.dto.UnifiedOrderRequest channelRequest = buildChannelRequest(order, config);
        PayChannelStrategy strategy = payChannelStrategyFactory.getStrategy(config.getChannelCode());
        long start = System.currentTimeMillis();
        UnifiedOrderResponse channelResponse = null;
        String errorMsg = null;
        try {
            channelResponse = strategy.unifiedOrder(channelRequest);
        } catch (Exception e) {
            errorMsg = e.getMessage();
            log.error("通道下单异常, orderNo={}, channel={}, error={}", orderNo, config.getChannelCode(), e.getMessage(), e);
        } finally {
            int costTime = (int) (System.currentTimeMillis() - start);
            if (strategy instanceof com.payhub.channel.strategy.AbstractPayChannel) {
                ((com.payhub.channel.strategy.AbstractPayChannel) strategy).saveChannelLog(
                        request.getMerchantNo(),
                        orderNo,
                        "unifiedOrder",
                        "",
                        JSON.toJSONString(channelRequest),
                        channelResponse != null ? JSON.toJSONString(channelResponse) : "",
                        channelResponse != null ? channelResponse.getChannelTradeNo() : "",
                        costTime,
                        errorMsg
                );
            }
        }

        if (channelResponse == null || !channelResponse.isSuccess()) {
            String finalError = channelResponse != null ? channelResponse.getMsg() : (errorMsg != null ? errorMsg : "通道下单失败");
            log.error("通道下单失败, orderNo={}, channel={}, error={}", 
                    orderNo, config.getChannelCode(), finalError);
            order.setPayStatus(PayStatusEnum.FAIL.getCode());
            this.updateById(order);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "支付通道下单失败: " + finalError);
        }

        order.setChannelTradeNo(channelResponse.getChannelTradeNo());
        this.updateById(order);

        log.info("通道下单成功: orderNo={}, channelTradeNo={}, channel={}", 
                orderNo, channelResponse.getChannelTradeNo(), config.getChannelCode());

        String finalPayParams = buildFinalPayParams(channelResponse.getPayType(), channelResponse.getPayParams(), order);

        return UnifiedOrderResponse.builder()
                .orderNo(orderNo)
                .payType(channelResponse.getPayType())
                .payParams(finalPayParams)
                .payStatus(PayStatusEnum.PENDING.getCode())
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

    private com.payhub.channel.dto.UnifiedOrderRequest buildChannelRequest(PayOrder order, MerchantPayConfig config) {
        com.payhub.channel.dto.UnifiedOrderRequest channelRequest = new com.payhub.channel.dto.UnifiedOrderRequest();
        channelRequest.setMerchantNo(order.getMerchantNo());
        channelRequest.setOrderNo(order.getOrderNo());
        channelRequest.setAmount(order.getPayAmount());
        channelRequest.setSubject(order.getProductSubject());
        channelRequest.setDetail(order.getProductDetail());
        channelRequest.setUserIdentity(order.getUserIdentity());
        channelRequest.setClientIp(order.getClientIp());
        channelRequest.setPayType(order.getPayType());
        channelRequest.setNotifyUrl(buildChannelNotifyUrl(config.getChannelCode()));

        if (StrUtil.isNotBlank(order.getExtraParams())) {
            try {
                Map<String, String> extraParamsMap = JSON.parseObject(order.getExtraParams(), Map.class);
                channelRequest.setExtraParams(extraParamsMap);
            } catch (Exception e) {
                log.warn("解析额外参数失败, orderNo={}, extraParams={}", order.getOrderNo(), order.getExtraParams());
            }
        }

        return channelRequest;
    }

    private String buildChannelNotifyUrl(String channelCode) {
        return "/api/pay/notify/" + channelCode.toLowerCase();
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

            notifyMerchantAsync(order);

            if (agentProfitService != null) {
                try {
                    BigDecimal feeAmount = order.getFeeAmount() != null ? order.getFeeAmount() : BigDecimal.ZERO;
                    agentProfitService.calculateProfit(order.getOrderNo(), order.getMerchantNo(),
                            order.getPayAmount(), feeAmount);
                    log.info("代理分润计算触发成功, orderNo={}", order.getOrderNo());
                } catch (Exception e) {
                    log.error("代理分润计算触发失败, orderNo={}", order.getOrderNo(), e);
                }
            }

            if (marketingDiscountService != null) {
                if (StrUtil.isNotBlank(order.getCouponCode())) {
                    try {
                        marketingDiscountService.recordCouponUse(
                                order.getOrderNo(),
                                order.getMerchantNo(),
                                order.getCouponCode(),
                                order.getPayAmount(),
                                order.getCouponDiscount(),
                                order.getUserIdentity()
                        );
                        log.info("支付成功-优惠券核销完成, orderNo={}, couponCode={}",
                                order.getOrderNo(), order.getCouponCode());
                    } catch (Exception e) {
                        log.error("支付成功-优惠券核销失败, orderNo={}, couponCode={}",
                                order.getOrderNo(), order.getCouponCode(), e);
                    }
                }
                if (StrUtil.isNotBlank(order.getLinkCode())) {
                    try {
                        marketingDiscountService.incrementPayLinkUsed(order.getLinkCode());
                        log.info("支付成功-支付链接使用次数+1, orderNo={}, linkCode={}",
                                order.getOrderNo(), order.getLinkCode());
                    } catch (Exception e) {
                        log.error("支付成功-支付链接使用次数递增失败, orderNo={}, linkCode={}",
                                order.getOrderNo(), order.getLinkCode(), e);
                    }
                }
            }
        } else if ("FAIL".equalsIgnoreCase(notifyResult.getPayStatus())
                || PayStatusEnum.FAIL.getCode().toString().equals(notifyResult.getPayStatus())) {
            order.setPayStatus(PayStatusEnum.FAIL.getCode());
            this.updateById(order);
            log.info("订单支付失败, orderNo={}", order.getOrderNo());

            notifyMerchantAsync(order);
        }

        return "success";
    }

    @Async
    public void notifyMerchantAsync(PayOrder order) {
        notifyMerchant(order);
    }

    private void notifyMerchant(PayOrder order) {
        if (StrUtil.isBlank(order.getNotifyUrl())) {
            log.info("商户通知地址为空, 跳过通知, orderNo={}", order.getOrderNo());
            return;
        }
        try {
            Map<String, Object> notifyParams = buildNotifyParams(order);
            String merchantSecret = getMerchantSecret(order.getMerchantNo());
            String sign = SignUtil.sign(notifyParams, DEFAULT_SIGN_TYPE, merchantSecret, null, null);
            notifyParams.put("sign", sign);
            notifyParams.put("signType", DEFAULT_SIGN_TYPE);

            log.info("通知商户, orderNo={}, notifyUrl={}, params={}", order.getOrderNo(), order.getNotifyUrl(), notifyParams);

            String response = HttpUtil.postJson(order.getNotifyUrl(), notifyParams);

            log.info("通知商户结果, orderNo={}, response={}", order.getOrderNo(), response);

            if (response == null || !"success".equalsIgnoreCase(response.trim())) {
                log.warn("商户通知响应异常, orderNo={}, response={}", order.getOrderNo(), response);
            }
        } catch (Exception e) {
            log.error("通知商户失败, orderNo={}, notifyUrl={}", order.getOrderNo(), order.getNotifyUrl(), e);
        }
    }

    private String getMerchantSecret(String merchantNo) {
        try {
            LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MerchantInfo::getMerchantNo, merchantNo)
                    .last("LIMIT 1");
            MerchantInfo merchantInfo = merchantInfoMapper.selectOne(wrapper);
            if (merchantInfo != null && StrUtil.isNotBlank(merchantInfo.getApiKeyMd5())) {
                String secret = Sm4Util.decrypt(merchantInfo.getApiKeyMd5());
                if (StrUtil.isNotBlank(secret)) {
                    return secret;
                }
            }
        } catch (Exception e) {
            log.warn("获取商户密钥失败, merchantNo={}, 使用默认密钥", merchantNo, e);
        }
        return DEFAULT_MERCHANT_SECRET;
    }

    @Override
    @Async
    public void simulateAsyncNotifyAfterDelay(PayOrder order) {
        if (order == null || StrUtil.isBlank(order.getOrderNo())) {
            return;
        }
        boolean sandboxMode = SandboxContext.isSandboxMode();
        String sandboxScene = SandboxContext.getScene();
        try {
            long delaySeconds = RandomUtil.randomLong(1, 4);
            log.info("沙箱环境模拟异步通知, orderNo={}, 延迟{}秒执行, sandboxMode={}, scene={}",
                    order.getOrderNo(), delaySeconds, sandboxMode, sandboxScene);
            TimeUnit.SECONDS.sleep(delaySeconds);

            if (sandboxMode) {
                SandboxContext.setSandboxMode(true);
                if (sandboxScene != null) {
                    SandboxContext.setScene(sandboxScene);
                }
            }

            Map<String, String> notifyParams = new HashMap<>();
            notifyParams.put("orderNo", order.getOrderNo());
            notifyParams.put("channelTradeNo", order.getChannelTradeNo());
            notifyParams.put("payStatus", "SUCCESS");
            notifyParams.put("payAmount", order.getPayAmount() != null ? order.getPayAmount().toString() : "0");

            String body = JSON.toJSONString(notifyParams);

            int notifyCount = 1;
            if (sandboxMode && com.payhub.channel.sandbox.SandboxSceneSimulator.shouldRepeatNotify()) {
                notifyCount = 3;
                log.info("沙箱场景：重复通知，将发送{}次通知", notifyCount);
            }

            for (int i = 0; i < notifyCount; i++) {
                log.info("沙箱环境触发模拟异步通知, orderNo={}, 第{}次", order.getOrderNo(), i + 1);
                try {
                    this.handleNotify(order.getPayChannel(), notifyParams, body);
                } catch (Exception e) {
                    log.warn("沙箱模拟异步通知第{}次失败, orderNo={}", i + 1, order.getOrderNo(), e);
                }
                if (i < notifyCount - 1) {
                    TimeUnit.SECONDS.sleep(2);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("沙箱模拟异步通知被中断, orderNo={}", order.getOrderNo());
        } catch (Exception e) {
            log.error("沙箱模拟异步通知失败, orderNo={}", order.getOrderNo(), e);
        } finally {
            SandboxContext.clear();
        }
    }

    private Map<String, Object> buildNotifyParams(PayOrder order) {
        Map<String, Object> params = new TreeMap<>();
        params.put("merchantNo", order.getMerchantNo());
        params.put("orderNo", order.getOrderNo());
        params.put("merchantOrderNo", order.getMerchantOrderNo());
        params.put("payStatus", order.getPayStatus());
        params.put("payAmount", order.getPayAmount());
        params.put("payChannel", order.getPayChannel());
        params.put("payType", order.getPayType());

        if (order.getChannelTradeNo() != null) {
            params.put("channelTradeNo", order.getChannelTradeNo());
        }
        if (order.getPayTime() != null) {
            params.put("payTime", order.getPayTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (order.getActualAmount() != null) {
            params.put("actualAmount", order.getActualAmount());
        }
        if (order.getFeeAmount() != null) {
            params.put("feeAmount", order.getFeeAmount());
        }

        return params;
    }
}
