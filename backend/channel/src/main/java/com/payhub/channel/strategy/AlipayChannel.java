package com.payhub.channel.strategy;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.payhub.channel.client.HttpUtil;
import com.payhub.channel.client.SignUtil;
import com.payhub.channel.config.ChannelProperties;
import com.payhub.channel.dto.*;
import com.payhub.channel.enums.PayChannelEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
public class AlipayChannel extends AbstractPayChannel {

    @Autowired
    private ChannelProperties channelProperties;

    @Override
    protected String getChannelCode() {
        return PayChannelEnum.ALIPAY.getCode();
    }

    @Override
    protected BigDecimal getDefaultAmount() {
        return new BigDecimal("100.00");
    }

    @Override
    public UnifiedOrderResponse unifiedOrder(UnifiedOrderRequest request) {
        log.info("[支付宝]开始统一下单, 订单号:{}, 金额:{}", request.getOrderNo(), request.getAmount());

        ChannelProperties.AlipayProperties config = channelProperties.getAlipay();

        Map<String, String> bizParams = new TreeMap<>();
        bizParams.put("out_trade_no", request.getOrderNo());
        bizParams.put("total_amount", request.getAmount().toString());
        bizParams.put("subject", request.getSubject());
        bizParams.put("body", StrUtil.isNotBlank(request.getDetail()) ? request.getDetail() : "");
        bizParams.put("product_code", getAlipayProductCode(request.getPayType()));
        bizParams.put("notify_url", StrUtil.isNotBlank(request.getNotifyUrl()) ? request.getNotifyUrl() : config.getNotifyUrl());

        if (StrUtil.isNotBlank(request.getUserIdentity())) {
            bizParams.put("buyer_id", request.getUserIdentity());
        }

        Map<String, String> params = new TreeMap<>();
        params.put("app_id", StrUtil.isNotBlank(request.getChannelAppId()) ? request.getChannelAppId() : config.getAppId());
        params.put("method", getAlipayApiMethod(request.getPayType()));
        params.put("charset", config.getCharset());
        params.put("sign_type", config.getSignType());
        params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("version", "1.0");
        params.put("notify_url", StrUtil.isNotBlank(request.getNotifyUrl()) ? request.getNotifyUrl() : config.getNotifyUrl());
        params.put("biz_content", JSON.toJSONString(bizParams));

        long startTime = System.currentTimeMillis();
        String requestData = JSON.toJSONString(params);

        try {
            if (config.getSandboxMode() == 1) {
                UnifiedOrderResponse response = buildUnifiedOrderResponse(request);
                saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "UNIFIED_ORDER",
                        config.getGatewayUrl(), requestData, JSON.toJSONString(response),
                        response.getChannelTradeNo(), (int) (System.currentTimeMillis() - startTime), null);
                return response;
            }

            String sign = SignUtil.alipaySign(params,
                    StrUtil.isNotBlank(request.getChannelSecretKey()) ? request.getChannelSecretKey() : config.getMerchantPrivateKey());
            params.put("sign", sign);

            String result = HttpUtil.postForm(config.getGatewayUrl(), params);
            JSONObject jsonObject = JSON.parseObject(result);
            String responseKey = getAlipayResponseKey(request.getPayType());
            JSONObject responseData = jsonObject.getJSONObject(responseKey);

            if (responseData == null) {
                saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "UNIFIED_ORDER",
                        config.getGatewayUrl(), requestData, result,
                        null, (int) (System.currentTimeMillis() - startTime), "支付宝响应为空");
                return UnifiedOrderResponse.fail("SYSTEM_ERROR", "支付宝响应异常");
            }

            if ("10000".equals(responseData.getString("code"))) {
                String payParams = extractPayParams(responseData, request.getPayType());
                String channelTradeNo = responseData.getString("trade_no");
                saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "UNIFIED_ORDER",
                        config.getGatewayUrl(), requestData, result,
                        channelTradeNo, (int) (System.currentTimeMillis() - startTime), null);
                return UnifiedOrderResponse.success(request.getPayType(), payParams, channelTradeNo);
            } else {
                saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "UNIFIED_ORDER",
                        config.getGatewayUrl(), requestData, result,
                        null, (int) (System.currentTimeMillis() - startTime), responseData.getString("sub_msg"));
                return UnifiedOrderResponse.fail(responseData.getString("sub_code"), responseData.getString("sub_msg"));
            }
        } catch (Exception e) {
            log.error("[支付宝]统一下单异常", e);
            saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "UNIFIED_ORDER",
                    config.getGatewayUrl(), requestData, null,
                    null, (int) (System.currentTimeMillis() - startTime), e.getMessage());
            return UnifiedOrderResponse.fail("SYSTEM_ERROR", "支付宝下单异常: " + e.getMessage());
        }
    }

    @Override
    public QueryOrderResponse queryOrder(String orderNo, String channelTradeNo) {
        log.info("[支付宝]开始查询订单, 订单号:{}, 通道交易号:{}", orderNo, channelTradeNo);

        ChannelProperties.AlipayProperties config = channelProperties.getAlipay();

        Map<String, String> bizParams = new TreeMap<>();
        if (StrUtil.isNotBlank(orderNo)) {
            bizParams.put("out_trade_no", orderNo);
        }
        if (StrUtil.isNotBlank(channelTradeNo)) {
            bizParams.put("trade_no", channelTradeNo);
        }

        Map<String, String> params = new TreeMap<>();
        params.put("app_id", config.getAppId());
        params.put("method", "alipay.trade.query");
        params.put("charset", config.getCharset());
        params.put("sign_type", config.getSignType());
        params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("version", "1.0");
        params.put("biz_content", JSON.toJSONString(bizParams));

        long startTime = System.currentTimeMillis();
        String requestData = JSON.toJSONString(params);

        try {
            if (config.getSandboxMode() == 1) {
                QueryOrderResponse response = buildQueryOrderResponse(orderNo, channelTradeNo);
                saveChannelLog(null, orderNo, "QUERY_ORDER",
                        config.getGatewayUrl(), requestData, JSON.toJSONString(response),
                        response.getChannelTradeNo(), (int) (System.currentTimeMillis() - startTime), null);
                return response;
            }

            String sign = SignUtil.alipaySign(params, config.getMerchantPrivateKey());
            params.put("sign", sign);

            String result = HttpUtil.postForm(config.getGatewayUrl(), params);
            JSONObject jsonObject = JSON.parseObject(result);
            JSONObject responseData = jsonObject.getJSONObject("alipay_trade_query_response");

            if (responseData == null) {
                saveChannelLog(null, orderNo, "QUERY_ORDER",
                        config.getGatewayUrl(), requestData, result,
                        null, (int) (System.currentTimeMillis() - startTime), "支付宝响应为空");
                return QueryOrderResponse.fail("SYSTEM_ERROR", "支付宝响应异常");
            }

            if ("10000".equals(responseData.getString("code"))) {
                String tradeStatus = responseData.getString("trade_status");
                String orderStatus = convertAlipayOrderStatus(tradeStatus);
                BigDecimal amount = new BigDecimal(responseData.getString("total_amount"));
                String tradeNo = responseData.getString("trade_no");
                LocalDateTime payTime = StrUtil.isNotBlank(responseData.getString("send_pay_date"))
                        ? LocalDateTime.parse(responseData.getString("send_pay_date"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        : null;

                saveChannelLog(null, orderNo, "QUERY_ORDER",
                        config.getGatewayUrl(), requestData, result,
                        tradeNo, (int) (System.currentTimeMillis() - startTime), null);
                return QueryOrderResponse.success(orderStatus, amount, tradeNo, payTime);
            } else {
                saveChannelLog(null, orderNo, "QUERY_ORDER",
                        config.getGatewayUrl(), requestData, result,
                        null, (int) (System.currentTimeMillis() - startTime), responseData.getString("sub_msg"));
                return QueryOrderResponse.fail(responseData.getString("sub_code"), responseData.getString("sub_msg"));
            }
        } catch (Exception e) {
            log.error("[支付宝]查询订单异常", e);
            saveChannelLog(null, orderNo, "QUERY_ORDER",
                    config.getGatewayUrl(), requestData, null,
                    null, (int) (System.currentTimeMillis() - startTime), e.getMessage());
            return QueryOrderResponse.fail("SYSTEM_ERROR", "支付宝查单异常: " + e.getMessage());
        }
    }

    @Override
    public RefundResponse refund(RefundRequest request) {
        log.info("[支付宝]开始退款, 订单号:{}, 退款单号:{}", request.getOrderNo(), request.getRefundNo());

        ChannelProperties.AlipayProperties config = channelProperties.getAlipay();

        Map<String, String> bizParams = new TreeMap<>();
        bizParams.put("out_trade_no", request.getOrderNo());
        bizParams.put("out_request_no", request.getRefundNo());
        bizParams.put("refund_amount", request.getRefundAmount().toString());
        bizParams.put("refund_reason", StrUtil.isNotBlank(request.getRefundReason()) ? request.getRefundReason() : "");
        if (StrUtil.isNotBlank(request.getChannelTradeNo())) {
            bizParams.put("trade_no", request.getChannelTradeNo());
        }

        Map<String, String> params = new TreeMap<>();
        params.put("app_id", config.getAppId());
        params.put("method", "alipay.trade.refund");
        params.put("charset", config.getCharset());
        params.put("sign_type", config.getSignType());
        params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("version", "1.0");
        params.put("biz_content", JSON.toJSONString(bizParams));

        long startTime = System.currentTimeMillis();
        String requestData = JSON.toJSONString(params);

        try {
            if (config.getSandboxMode() == 1) {
                RefundResponse response = buildRefundResponse(request);
                saveChannelLog(null, request.getOrderNo(), "REFUND",
                        config.getGatewayUrl(), requestData, JSON.toJSONString(response),
                        response.getChannelRefundNo(), (int) (System.currentTimeMillis() - startTime), null);
                return response;
            }

            String sign = SignUtil.alipaySign(params,
                    StrUtil.isNotBlank(request.getChannelSecretKey()) ? request.getChannelSecretKey() : config.getMerchantPrivateKey());
            params.put("sign", sign);

            String result = HttpUtil.postForm(config.getGatewayUrl(), params);
            JSONObject jsonObject = JSON.parseObject(result);
            JSONObject responseData = jsonObject.getJSONObject("alipay_trade_refund_response");

            if (responseData == null) {
                saveChannelLog(null, request.getOrderNo(), "REFUND",
                        config.getGatewayUrl(), requestData, result,
                        null, (int) (System.currentTimeMillis() - startTime), "支付宝响应为空");
                return RefundResponse.fail("SYSTEM_ERROR", "支付宝响应异常");
            }

            if ("10000".equals(responseData.getString("code"))) {
                String channelRefundNo = responseData.getString("out_request_no");
                String refundStatus = "SUCCESS".equalsIgnoreCase(responseData.getString("refund_status")) ? "SUCCESS" : "PROCESSING";
                saveChannelLog(null, request.getOrderNo(), "REFUND",
                        config.getGatewayUrl(), requestData, result,
                        channelRefundNo, (int) (System.currentTimeMillis() - startTime), null);
                return RefundResponse.success(channelRefundNo, refundStatus);
            } else {
                saveChannelLog(null, request.getOrderNo(), "REFUND",
                        config.getGatewayUrl(), requestData, result,
                        null, (int) (System.currentTimeMillis() - startTime), responseData.getString("sub_msg"));
                return RefundResponse.fail(responseData.getString("sub_code"), responseData.getString("sub_msg"));
            }
        } catch (Exception e) {
            log.error("[支付宝]退款异常", e);
            saveChannelLog(null, request.getOrderNo(), "REFUND",
                    config.getGatewayUrl(), requestData, null,
                    null, (int) (System.currentTimeMillis() - startTime), e.getMessage());
            return RefundResponse.fail("SYSTEM_ERROR", "支付宝退款异常: " + e.getMessage());
        }
    }

    @Override
    public QueryRefundResponse queryRefund(String refundNo, String channelRefundNo) {
        log.info("[支付宝]开始查询退款, 退款单号:{}, 通道退款号:{}", refundNo, channelRefundNo);

        ChannelProperties.AlipayProperties config = channelProperties.getAlipay();

        Map<String, String> bizParams = new TreeMap<>();
        bizParams.put("out_request_no", StrUtil.isNotBlank(refundNo) ? refundNo : channelRefundNo);
        if (StrUtil.isNotBlank(channelRefundNo)) {
            bizParams.put("trade_no", channelRefundNo);
        }

        Map<String, String> params = new TreeMap<>();
        params.put("app_id", config.getAppId());
        params.put("method", "alipay.trade.fastpay.refund.query");
        params.put("charset", config.getCharset());
        params.put("sign_type", config.getSignType());
        params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("version", "1.0");
        params.put("biz_content", JSON.toJSONString(bizParams));

        long startTime = System.currentTimeMillis();
        String requestData = JSON.toJSONString(params);

        try {
            if (config.getSandboxMode() == 1) {
                QueryRefundResponse response = buildQueryRefundResponse(refundNo, channelRefundNo, null);
                saveChannelLog(null, null, "QUERY_REFUND",
                        config.getGatewayUrl(), requestData, JSON.toJSONString(response),
                        response.getChannelRefundNo(), (int) (System.currentTimeMillis() - startTime), null);
                return response;
            }

            String sign = SignUtil.alipaySign(params, config.getMerchantPrivateKey());
            params.put("sign", sign);

            String result = HttpUtil.postForm(config.getGatewayUrl(), params);
            JSONObject jsonObject = JSON.parseObject(result);
            JSONObject responseData = jsonObject.getJSONObject("alipay_trade_fastpay_refund_query_response");

            if (responseData == null) {
                saveChannelLog(null, null, "QUERY_REFUND",
                        config.getGatewayUrl(), requestData, result,
                        null, (int) (System.currentTimeMillis() - startTime), "支付宝响应为空");
                return QueryRefundResponse.fail("SYSTEM_ERROR", "支付宝响应异常");
            }

            if ("10000".equals(responseData.getString("code"))) {
                String refundStatus = convertAlipayRefundStatus(responseData.getString("refund_status"));
                BigDecimal refundAmount = new BigDecimal(responseData.getString("refund_amount"));
                String refundChannelNo = responseData.getString("out_request_no");
                LocalDateTime refundTime = LocalDateTime.now();

                saveChannelLog(null, null, "QUERY_REFUND",
                        config.getGatewayUrl(), requestData, result,
                        refundChannelNo, (int) (System.currentTimeMillis() - startTime), null);
                return QueryRefundResponse.success(refundStatus, refundAmount, refundChannelNo, refundTime);
            } else {
                saveChannelLog(null, null, "QUERY_REFUND",
                        config.getGatewayUrl(), requestData, result,
                        null, (int) (System.currentTimeMillis() - startTime), responseData.getString("sub_msg"));
                return QueryRefundResponse.fail(responseData.getString("sub_code"), responseData.getString("sub_msg"));
            }
        } catch (Exception e) {
            log.error("[支付宝]查询退款异常", e);
            saveChannelLog(null, null, "QUERY_REFUND",
                    config.getGatewayUrl(), requestData, null,
                    null, (int) (System.currentTimeMillis() - startTime), e.getMessage());
            return QueryRefundResponse.fail("SYSTEM_ERROR", "支付宝查询退款异常: " + e.getMessage());
        }
    }

    @Override
    public NotifyResult parseNotify(String notifyData, Map<String, String> params) {
        log.info("[支付宝]解析支付回调通知");

        NotifyResult result = new NotifyResult();

        if (StrUtil.isNotBlank(notifyData)) {
            JSONObject jsonObject = JSON.parseObject(notifyData);
            result.setOrderNo(jsonObject.getString("out_trade_no"));
            result.setChannelTradeNo(jsonObject.getString("trade_no"));
            result.setPayStatus(convertAlipayOrderStatus(jsonObject.getString("trade_status")));
            result.setPayAmount(new BigDecimal(jsonObject.getString("total_amount")));
            result.setMerchantNo(jsonObject.getString("seller_id"));

            String gmtPayment = jsonObject.getString("gmt_payment");
            if (StrUtil.isNotBlank(gmtPayment)) {
                result.setPayTime(LocalDateTime.parse(gmtPayment, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
        } else if (params != null && !params.isEmpty()) {
            result.setOrderNo(params.get("out_trade_no"));
            result.setChannelTradeNo(params.get("trade_no"));
            result.setPayStatus(convertAlipayOrderStatus(params.get("trade_status")));
            result.setPayAmount(new BigDecimal(params.getOrDefault("total_amount", "0")));
            result.setMerchantNo(params.get("seller_id"));

            String gmtPayment = params.get("gmt_payment");
            if (StrUtil.isNotBlank(gmtPayment)) {
                result.setPayTime(LocalDateTime.parse(gmtPayment, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
        } else {
            result.setOrderNo("SANDBOX_ORDER_" + IdUtil.getSnowflakeNextIdStr());
            result.setChannelTradeNo(generateChannelTradeNo());
            result.setPayStatus("SUCCESS");
            result.setPayAmount(getDefaultAmount());
            result.setPayTime(LocalDateTime.now());
            result.setMerchantNo("sandbox_merchant");
        }

        log.info("[支付宝]回调解析结果:{}", JSON.toJSONString(result));
        return result;
    }

    @Override
    public boolean verifyNotify(Map<String, String> params) {
        if (channelProperties.getAlipay().getSandboxMode() == 1) {
            return super.verifyNotify(params);
        }
        return SignUtil.alipayVerify(params, channelProperties.getAlipay().getAlipayPublicKey());
    }

    private String getAlipayProductCode(String payType) {
        if ("H5".equalsIgnoreCase(payType)) {
            return "QUICK_WAP_WAY";
        } else if ("NATIVE".equalsIgnoreCase(payType)) {
            return "FACE_TO_FACE_PAYMENT";
        } else if ("JSAPI".equalsIgnoreCase(payType)) {
            return "JSAPI_PAY";
        } else if ("APP".equalsIgnoreCase(payType)) {
            return "QUICK_MSECURITY_PAY";
        }
        return "QUICK_WAP_WAY";
    }

    private String getAlipayApiMethod(String payType) {
        if ("H5".equalsIgnoreCase(payType)) {
            return "alipay.trade.wap.pay";
        } else if ("NATIVE".equalsIgnoreCase(payType)) {
            return "alipay.trade.precreate";
        } else if ("JSAPI".equalsIgnoreCase(payType)) {
            return "alipay.trade.create";
        } else if ("APP".equalsIgnoreCase(payType)) {
            return "alipay.trade.app.pay";
        }
        return "alipay.trade.page.pay";
    }

    private String getAlipayResponseKey(String payType) {
        if ("H5".equalsIgnoreCase(payType)) {
            return "alipay_trade_wap_pay_response";
        } else if ("NATIVE".equalsIgnoreCase(payType)) {
            return "alipay_trade_precreate_response";
        } else if ("JSAPI".equalsIgnoreCase(payType)) {
            return "alipay_trade_create_response";
        } else if ("APP".equalsIgnoreCase(payType)) {
            return "alipay_trade_app_pay_response";
        }
        return "alipay_trade_page_pay_response";
    }

    private String extractPayParams(JSONObject responseData, String payType) {
        if ("NATIVE".equalsIgnoreCase(payType)) {
            return responseData.getString("qr_code");
        } else if ("H5".equalsIgnoreCase(payType)) {
            return responseData.getString("pay_url");
        } else if ("APP".equalsIgnoreCase(payType)) {
            return responseData.toJSONString();
        } else if ("JSAPI".equalsIgnoreCase(payType)) {
            return responseData.toJSONString();
        }
        return responseData.toJSONString();
    }

    private String convertAlipayOrderStatus(String alipayStatus) {
        if (StrUtil.isBlank(alipayStatus)) {
            return "PENDING";
        }
        switch (alipayStatus) {
            case "TRADE_SUCCESS":
            case "TRADE_FINISHED":
                return "SUCCESS";
            case "WAIT_BUYER_PAY":
                return "PENDING";
            case "TRADE_CLOSED":
                return "CLOSED";
            default:
                return "PENDING";
        }
    }

    private String convertAlipayRefundStatus(String alipayStatus) {
        if (StrUtil.isBlank(alipayStatus)) {
            return "PROCESSING";
        }
        switch (alipayStatus) {
            case "REFUND_SUCCESS":
                return "SUCCESS";
            case "REFUND_FAIL":
                return "FAILED";
            case "PROCESSING":
                return "PROCESSING";
            default:
                return "PROCESSING";
        }
    }
}
