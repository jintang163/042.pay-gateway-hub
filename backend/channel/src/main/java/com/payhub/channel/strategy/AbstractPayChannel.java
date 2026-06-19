package com.payhub.channel.strategy;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.payhub.channel.dto.*;
import com.payhub.channel.entity.PayChannelLog;
import com.payhub.channel.enums.OrderStatusEnum;
import com.payhub.channel.enums.PayTypeEnum;
import com.payhub.channel.enums.RefundStatusEnum;
import com.payhub.channel.mapper.PayChannelLogMapper;
import com.payhub.channel.sandbox.SandboxSceneSimulator;
import com.payhub.common.context.SandboxContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractPayChannel implements PayChannelStrategy {

    protected static PayChannelLogMapper payChannelLogMapper;

    @Autowired
    public void setPayChannelLogMapper(PayChannelLogMapper mapper) {
        AbstractPayChannel.payChannelLogMapper = mapper;
    }

    protected static final Map<String, LocalDateTime> PAY_TIME_CACHE = new ConcurrentHashMap<>();

    protected abstract String getChannelCode();

    protected abstract BigDecimal getDefaultAmount();

    protected String generateChannelTradeNo() {
        return getChannelCode() + IdUtil.getSnowflakeNextIdStr();
    }

    protected String generateChannelRefundNo() {
        return "RF" + getChannelCode() + IdUtil.getSnowflakeNextIdStr();
    }

    protected LocalDateTime getPayTime(String orderNo) {
        LocalDateTime payTime = PAY_TIME_CACHE.get(orderNo);
        if (payTime == null) {
            payTime = LocalDateTime.now().minusMinutes(2);
            PAY_TIME_CACHE.put(orderNo, payTime);
        }
        return payTime;
    }

    protected UnifiedOrderResponse buildUnifiedOrderResponse(UnifiedOrderRequest request) {
        if (SandboxContext.isSandboxMode()) {
            return SandboxSceneSimulator.simulateUnifiedOrder(request, () -> doBuildUnifiedOrderResponse(request));
        }
        return doBuildUnifiedOrderResponse(request);
    }

    private UnifiedOrderResponse doBuildUnifiedOrderResponse(UnifiedOrderRequest request) {
        String payType = request.getPayType();
        String channelTradeNo = generateChannelTradeNo();
        String payParams;
        String expireTime = LocalDateTime.now().plusMinutes(30).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        PayTypeEnum payTypeEnum = PayTypeEnum.getByCode(payType);
        if (payTypeEnum == null) {
            payTypeEnum = PayTypeEnum.NATIVE;
        }

        switch (payTypeEnum) {
            case H5:
                Map<String, String> h5Params = new ConcurrentHashMap<>();
                h5Params.put("h5Url", "https://sandbox." + getChannelCode().toLowerCase() + ".com/pay/h5?tradeNo=" + channelTradeNo + "&amount=" + request.getAmount());
                h5Params.put("expireTime", expireTime);
                payParams = JSON.toJSONString(h5Params);
                break;
            case NATIVE:
                Map<String, String> nativeParams = new ConcurrentHashMap<>();
                nativeParams.put("qrCode", "weixin://wxpay/sandbox/qrcode/" + channelTradeNo);
                nativeParams.put("qrCodeUrl", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");
                nativeParams.put("expireTime", expireTime);
                payParams = JSON.toJSONString(nativeParams);
                break;
            case JSAPI:
                Map<String, String> jsapiParams = new ConcurrentHashMap<>();
                jsapiParams.put("appId", StrUtil.isNotBlank(request.getChannelAppId()) ? request.getChannelAppId() : "sandbox_app_id");
                jsapiParams.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
                jsapiParams.put("nonceStr", IdUtil.fastSimpleUUID());
                jsapiParams.put("packageVal", "prepay_id=" + channelTradeNo);
                jsapiParams.put("signType", "RSA");
                jsapiParams.put("paySign", "sandbox_sign_" + IdUtil.fastSimpleUUID());
                payParams = JSON.toJSONString(jsapiParams);
                break;
            case APP:
                Map<String, String> appParams = new ConcurrentHashMap<>();
                appParams.put("appId", StrUtil.isNotBlank(request.getChannelAppId()) ? request.getChannelAppId() : "sandbox_app_id");
                appParams.put("partnerId", StrUtil.isNotBlank(request.getChannelMerchantId()) ? request.getChannelMerchantId() : "sandbox_mch_id");
                appParams.put("prepayId", channelTradeNo);
                appParams.put("packageVal", "Sign=WXPay");
                appParams.put("nonceStr", IdUtil.fastSimpleUUID());
                appParams.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
                appParams.put("sign", "sandbox_sign_" + IdUtil.fastSimpleUUID());
                payParams = JSON.toJSONString(appParams);
                break;
            default:
                Map<String, String> defaultParams = new ConcurrentHashMap<>();
                defaultParams.put("payUrl", "https://sandbox." + getChannelCode().toLowerCase() + ".com/pay/default?tradeNo=" + channelTradeNo);
                defaultParams.put("expireTime", expireTime);
                payParams = JSON.toJSONString(defaultParams);
        }

        log.info("[{}]沙箱下单成功, 订单号:{}, 通道交易号:{}, 支付方式:{}", getChannelCode(), request.getOrderNo(), channelTradeNo, payType);
        return UnifiedOrderResponse.success(payType, payParams, channelTradeNo);
    }

    protected QueryOrderResponse buildQueryOrderResponse(String orderNo, String channelTradeNo) {
        if (SandboxContext.isSandboxMode()) {
            return SandboxSceneSimulator.simulateQueryOrder(orderNo, channelTradeNo,
                    () -> doBuildQueryOrderResponse(orderNo, channelTradeNo));
        }
        return doBuildQueryOrderResponse(orderNo, channelTradeNo);
    }

    private QueryOrderResponse doBuildQueryOrderResponse(String orderNo, String channelTradeNo) {
        LocalDateTime payTime = getPayTime(orderNo);
        log.info("[{}]沙箱查单成功, 订单号:{}, 通道交易号:{}, 状态:{}", getChannelCode(), orderNo, channelTradeNo, OrderStatusEnum.SUCCESS.getCode());
        return QueryOrderResponse.success(
                OrderStatusEnum.SUCCESS.getCode(),
                getDefaultAmount(),
                StrUtil.isNotBlank(channelTradeNo) ? channelTradeNo : generateChannelTradeNo(),
                payTime
        );
    }

    protected RefundResponse buildRefundResponse(RefundRequest request) {
        if (SandboxContext.isSandboxMode()) {
            return SandboxSceneSimulator.simulateRefund(request,
                    () -> doBuildRefundResponse(request));
        }
        return doBuildRefundResponse(request);
    }

    private RefundResponse doBuildRefundResponse(RefundRequest request) {
        String channelRefundNo = generateChannelRefundNo();
        log.info("[{}]沙箱退款成功, 订单号:{}, 退款单号:{}, 通道退款号:{}", getChannelCode(), request.getOrderNo(), request.getRefundNo(), channelRefundNo);
        return RefundResponse.success(channelRefundNo, RefundStatusEnum.SUCCESS.getCode());
    }

    protected QueryRefundResponse buildQueryRefundResponse(String refundNo, String channelRefundNo, BigDecimal refundAmount) {
        log.info("[{}]沙箱退款查询成功, 退款单号:{}, 通道退款号:{}", getChannelCode(), refundNo, channelRefundNo);
        return QueryRefundResponse.success(
                RefundStatusEnum.SUCCESS.getCode(),
                refundAmount != null ? refundAmount : getDefaultAmount(),
                StrUtil.isNotBlank(channelRefundNo) ? channelRefundNo : generateChannelRefundNo(),
                LocalDateTime.now().minusMinutes(1)
        );
    }

    protected void saveChannelLog(String merchantNo, String orderNo, String requestType,
                                   String requestUrl, String requestData, String responseData,
                                   String channelTradeNo, Integer costTime, String errorMsg) {
        try {
            PayChannelLog channelLog = new PayChannelLog();
            channelLog.setMerchantNo(merchantNo);
            channelLog.setOrderNo(orderNo);
            channelLog.setChannelCode(getChannelCode());
            channelLog.setRequestType(requestType);
            channelLog.setRequestUrl(requestUrl);
            channelLog.setRequestData(requestData);
            channelLog.setResponseData(responseData);
            channelLog.setChannelTradeNo(channelTradeNo);
            channelLog.setCostTime(costTime);
            channelLog.setErrorMsg(errorMsg);
            channelLog.setCreateTime(LocalDateTime.now());
            if (payChannelLogMapper != null) {
                payChannelLogMapper.insert(channelLog);
                log.info("[{}]通道日志已保存, orderNo={}, requestType={}, costTime={}ms, success={}",
                        getChannelCode(), orderNo, requestType, costTime, StrUtil.isBlank(errorMsg));
            } else {
                log.warn("[{}]payChannelLogMapper未注入，跳过通道日志持久化: {}", getChannelCode(), JSON.toJSONString(channelLog));
            }
        } catch (Exception e) {
            log.warn("[{}]保存通道日志失败:{}", getChannelCode(), e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyNotify(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return false;
        }
        if (SandboxContext.isSandboxMode()) {
            boolean defaultResult = true;
            log.info("[{}]沙箱模式，回调验签默认通过", getChannelCode());
            return SandboxSceneSimulator.verifyNotifyInSandbox(defaultResult);
        }
        return true;
    }

    @Override
    public ChannelReconcileBill downloadReconcileBill(LocalDate billDate, String merchantNo) {
        log.info("[{}]开始下载对账单, 日期:{}, 商户:{}", getChannelCode(), billDate, merchantNo);

        ChannelReconcileBill bill = new ChannelReconcileBill();
        bill.setPayChannel(getChannelCode());
        bill.setBillDate(billDate.toString());
        if (StrUtil.isNotBlank(merchantNo)) {
            bill.setMerchantNo(merchantNo);
        }

        List<ChannelReconcileBill.ChannelReconcileItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        int itemCount = 3 + (int) (Math.random() * 5);
        for (int i = 0; i < itemCount; i++) {
            ChannelReconcileBill.ChannelReconcileItem item = new ChannelReconcileBill.ChannelReconcileItem();
            BigDecimal amount = getDefaultAmount().multiply(new BigDecimal(1 + Math.random()));
            String channelTradeNo = generateChannelTradeNo();
            item.setChannelTradeNo(channelTradeNo);
            item.setMerchantNo(StrUtil.isNotBlank(merchantNo) ? merchantNo : "M" + String.format("%06d", i + 1));
            item.setMerchantOrderNo("MO" + billDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + String.format("%06d", i + 1));
            item.setTradeAmount(amount.setScale(2, java.math.RoundingMode.HALF_UP));
            item.setTradeStatus(OrderStatusEnum.SUCCESS.getDesc());
            item.setTradeTime(LocalDateTime.of(billDate, java.time.LocalTime.of(9 + i, 0)));
            item.setFeeAmount(amount.multiply(new BigDecimal("0.006")).setScale(2, java.math.RoundingMode.HALF_UP));
            item.setBuyerAccount("buyer_" + i + "@example.com");

            if (Math.random() < 0.08) {
                item.setTradeAmount(amount.multiply(new BigDecimal("0.95")).setScale(2, java.math.RoundingMode.HALF_UP));
            }
            if (Math.random() < 0.05) {
                item.setTradeStatus(OrderStatusEnum.FAILED.getDesc());
            }

            PAY_TIME_CACHE.put("BILL_" + channelTradeNo, item.getTradeTime());

            items.add(item);
            totalAmount = totalAmount.add(item.getTradeAmount());
        }

        bill.setItems(items);
        bill.setTotalCount(items.size());
        bill.setTotalAmount(totalAmount);

        log.info("[{}]对账单下载完成, 商户:{}, 总笔数:{}, 总金额:{}", getChannelCode(), merchantNo, items.size(), totalAmount);
        return bill;
    }
}
