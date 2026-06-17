package com.payhub.channel.strategy;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.payhub.channel.dto.*;
import com.payhub.channel.entity.PayChannelLog;
import com.payhub.channel.enums.OrderStatusEnum;
import com.payhub.channel.enums.PayTypeEnum;
import com.payhub.channel.enums.RefundStatusEnum;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractPayChannel implements PayChannelStrategy {

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
        String payType = request.getPayType();
        String channelTradeNo = generateChannelTradeNo();
        String payParams;

        PayTypeEnum payTypeEnum = PayTypeEnum.getByCode(payType);
        if (payTypeEnum == null) {
            payTypeEnum = PayTypeEnum.NATIVE;
        }

        switch (payTypeEnum) {
            case H5:
                payParams = "https://sandbox." + getChannelCode().toLowerCase() + ".com/pay/h5?tradeNo=" + channelTradeNo + "&amount=" + request.getAmount();
                break;
            case NATIVE:
                payParams = "weixin://wxpay/sandbox/qrcode/" + channelTradeNo;
                break;
            case JSAPI:
                Map<String, String> jsapiParams = new ConcurrentHashMap<>();
                jsapiParams.put("appId", StrUtil.isNotBlank(request.getChannelAppId()) ? request.getChannelAppId() : "sandbox_app_id");
                jsapiParams.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
                jsapiParams.put("nonceStr", IdUtil.fastSimpleUUID());
                jsapiParams.put("package", "prepay_id=" + channelTradeNo);
                jsapiParams.put("signType", "RSA");
                jsapiParams.put("paySign", "sandbox_sign_" + IdUtil.fastSimpleUUID());
                payParams = JSON.toJSONString(jsapiParams);
                break;
            case APP:
                Map<String, String> appParams = new ConcurrentHashMap<>();
                appParams.put("appId", StrUtil.isNotBlank(request.getChannelAppId()) ? request.getChannelAppId() : "sandbox_app_id");
                appParams.put("partnerId", StrUtil.isNotBlank(request.getChannelMerchantId()) ? request.getChannelMerchantId() : "sandbox_mch_id");
                appParams.put("prepayId", channelTradeNo);
                appParams.put("packageValue", "Sign=WXPay");
                appParams.put("nonceStr", IdUtil.fastSimpleUUID());
                appParams.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
                appParams.put("sign", "sandbox_sign_" + IdUtil.fastSimpleUUID());
                payParams = JSON.toJSONString(appParams);
                break;
            default:
                payParams = "https://sandbox." + getChannelCode().toLowerCase() + ".com/pay/default?tradeNo=" + channelTradeNo;
        }

        log.info("[{}]沙箱下单成功, 订单号:{}, 通道交易号:{}, 支付方式:{}", getChannelCode(), request.getOrderNo(), channelTradeNo, payType);
        return UnifiedOrderResponse.success(payType, payParams, channelTradeNo);
    }

    protected QueryOrderResponse buildQueryOrderResponse(String orderNo, String channelTradeNo) {
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
            log.debug("[{}]通道日志:{}", getChannelCode(), JSON.toJSONString(channelLog));
        } catch (Exception e) {
            log.warn("[{}]保存通道日志失败:{}", getChannelCode(), e.getMessage());
        }
    }

    @Override
    public boolean verifyNotify(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return false;
        }
        log.info("[{}]沙箱模式，回调验签默认通过", getChannelCode());
        return true;
    }
}
