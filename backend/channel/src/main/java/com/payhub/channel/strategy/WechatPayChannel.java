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
import com.payhub.common.context.SandboxContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
public class WechatPayChannel extends AbstractPayChannel {

    @Autowired
    private ChannelProperties channelProperties;

    private static final String SUCCESS_CODE = "SUCCESS";
    private static final String FAIL_CODE = "FAIL";

    @Override
    protected String getChannelCode() {
        return PayChannelEnum.WECHAT_PAY.getCode();
    }

    @Override
    protected BigDecimal getDefaultAmount() {
        return new BigDecimal("88.88");
    }

    @Override
    public UnifiedOrderResponse unifiedOrder(UnifiedOrderRequest request) {
        log.info("[微信支付]开始统一下单, 订单号:{}, 金额:{}", request.getOrderNo(), request.getAmount());

        ChannelProperties.WechatProperties config = channelProperties.getWechat();
        String appId = StrUtil.isNotBlank(request.getChannelAppId()) ? request.getChannelAppId() : config.getAppId();
        String mchId = StrUtil.isNotBlank(request.getChannelMerchantId()) ? request.getChannelMerchantId() : config.getMchId();
        String apiKey = StrUtil.isNotBlank(request.getChannelSecretKey()) ? request.getChannelSecretKey() : config.getApiKey();

        Map<String, String> params = new TreeMap<>();
        params.put("appid", appId);
        params.put("mch_id", mchId);
        params.put("nonce_str", IdUtil.fastSimpleUUID());
        params.put("body", request.getSubject());
        params.put("out_trade_no", request.getOrderNo());
        params.put("total_fee", request.getAmount().multiply(new BigDecimal("100")).intValue() + "");
        params.put("spbill_create_ip", StrUtil.isNotBlank(request.getClientIp()) ? request.getClientIp() : "127.0.0.1");
        params.put("notify_url", StrUtil.isNotBlank(request.getNotifyUrl()) ? request.getNotifyUrl() : config.getNotifyUrl());
        params.put("trade_type", request.getPayType());

        if ("JSAPI".equalsIgnoreCase(request.getPayType()) && StrUtil.isNotBlank(request.getUserIdentity())) {
            params.put("openid", request.getUserIdentity());
        }

        long startTime = System.currentTimeMillis();
        String requestData = JSON.toJSONString(params);
        String gatewayUrl = config.getGatewayUrl() + getWechatApiPath(request.getPayType());

        try {
            if (SandboxContext.isSandboxMode() || config.getSandboxMode() == 1) {
                UnifiedOrderResponse response = buildUnifiedOrderResponse(request);
                saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "UNIFIED_ORDER",
                        gatewayUrl, requestData, JSON.toJSONString(response),
                        response.getChannelTradeNo(), (int) (System.currentTimeMillis() - startTime), null);
                return response;
            }

            params.put("sign", SignUtil.wechatSign(params, apiKey));
            String xmlParams = mapToXml(params);
            String result = HttpUtil.postXml(gatewayUrl, xmlParams);
            Map<String, String> resultMap = xmlToMap(result);

            if (!SUCCESS_CODE.equals(resultMap.get("return_code"))) {
                saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "UNIFIED_ORDER",
                        gatewayUrl, requestData, result,
                        null, (int) (System.currentTimeMillis() - startTime), resultMap.get("return_msg"));
                return UnifiedOrderResponse.fail(FAIL_CODE, resultMap.getOrDefault("return_msg", "微信支付下单失败"));
            }

            if (!SUCCESS_CODE.equals(resultMap.get("result_code"))) {
                saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "UNIFIED_ORDER",
                        gatewayUrl, requestData, result,
                        null, (int) (System.currentTimeMillis() - startTime), resultMap.get("err_code_des"));
                return UnifiedOrderResponse.fail(resultMap.get("err_code"), resultMap.getOrDefault("err_code_des", "微信支付下单失败"));
            }

            String channelTradeNo = resultMap.get("prepay_id");
            String payParams = extractWechatPayParams(resultMap, request.getPayType(), appId, apiKey);
            saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "UNIFIED_ORDER",
                    gatewayUrl, requestData, result,
                    channelTradeNo, (int) (System.currentTimeMillis() - startTime), null);
            return UnifiedOrderResponse.success(request.getPayType(), payParams, channelTradeNo);
        } catch (Exception e) {
            log.error("[微信支付]统一下单异常", e);
            saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "UNIFIED_ORDER",
                    gatewayUrl, requestData, null,
                    null, (int) (System.currentTimeMillis() - startTime), e.getMessage());
            return UnifiedOrderResponse.fail("SYSTEM_ERROR", "微信支付下单异常: " + e.getMessage());
        }
    }

    @Override
    public QueryOrderResponse queryOrder(String orderNo, String channelTradeNo) {
        log.info("[微信支付]开始查询订单, 订单号:{}, 通道交易号:{}", orderNo, channelTradeNo);

        ChannelProperties.WechatProperties config = channelProperties.getWechat();
        Map<String, String> params = new TreeMap<>();
        params.put("appid", config.getAppId());
        params.put("mch_id", config.getMchId());
        params.put("nonce_str", IdUtil.fastSimpleUUID());
        if (StrUtil.isNotBlank(orderNo)) {
            params.put("out_trade_no", orderNo);
        }
        if (StrUtil.isNotBlank(channelTradeNo)) {
            params.put("transaction_id", channelTradeNo);
        }

        long startTime = System.currentTimeMillis();
        String requestData = JSON.toJSONString(params);
        String gatewayUrl = config.getGatewayUrl() + "/pay/orderquery";

        try {
            if (SandboxContext.isSandboxMode() || config.getSandboxMode() == 1) {
                QueryOrderResponse response = buildQueryOrderResponse(orderNo, channelTradeNo);
                saveChannelLog(null, orderNo, "QUERY_ORDER",
                        gatewayUrl, requestData, JSON.toJSONString(response),
                        response.getChannelTradeNo(), (int) (System.currentTimeMillis() - startTime), null);
                return response;
            }

            params.put("sign", SignUtil.wechatSign(params, config.getApiKey()));
            String xmlParams = mapToXml(params);
            String result = HttpUtil.postXml(gatewayUrl, xmlParams);
            Map<String, String> resultMap = xmlToMap(result);

            if (!SUCCESS_CODE.equals(resultMap.get("return_code"))) {
                saveChannelLog(null, orderNo, "QUERY_ORDER",
                        gatewayUrl, requestData, result,
                        null, (int) (System.currentTimeMillis() - startTime), resultMap.get("return_msg"));
                return QueryOrderResponse.fail(FAIL_CODE, resultMap.getOrDefault("return_msg", "微信支付查单失败"));
            }

            String tradeState = resultMap.get("trade_state");
            String orderStatus = convertWechatOrderStatus(tradeState);
            BigDecimal amount = new BigDecimal(resultMap.getOrDefault("total_fee", "0"))
                    .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
            String transactionId = resultMap.get("transaction_id");
            LocalDateTime payTime = null;
            String timeEnd = resultMap.get("time_end");
            if (StrUtil.isNotBlank(timeEnd)) {
                payTime = LocalDateTime.parse(timeEnd, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            }

            saveChannelLog(null, orderNo, "QUERY_ORDER",
                    gatewayUrl, requestData, result,
                    transactionId, (int) (System.currentTimeMillis() - startTime), null);
            return QueryOrderResponse.success(orderStatus, amount, transactionId, payTime);
        } catch (Exception e) {
            log.error("[微信支付]查询订单异常", e);
            saveChannelLog(null, orderNo, "QUERY_ORDER",
                    gatewayUrl, requestData, null,
                    null, (int) (System.currentTimeMillis() - startTime), e.getMessage());
            return QueryOrderResponse.fail("SYSTEM_ERROR", "微信支付查单异常: " + e.getMessage());
        }
    }

    @Override
    public RefundResponse refund(RefundRequest request) {
        log.info("[微信支付]开始退款, 订单号:{}, 退款单号:{}", request.getOrderNo(), request.getRefundNo());

        ChannelProperties.WechatProperties config = channelProperties.getWechat();
        String apiKey = StrUtil.isNotBlank(request.getChannelSecretKey()) ? request.getChannelSecretKey() : config.getApiKey();

        Map<String, String> params = new TreeMap<>();
        params.put("appid", config.getAppId());
        params.put("mch_id", StrUtil.isNotBlank(request.getChannelMerchantId()) ? request.getChannelMerchantId() : config.getMchId());
        params.put("nonce_str", IdUtil.fastSimpleUUID());
        params.put("out_trade_no", request.getOrderNo());
        params.put("out_refund_no", request.getRefundNo());
        params.put("total_fee", "1");
        params.put("refund_fee", request.getRefundAmount().multiply(new BigDecimal("100")).intValue() + "");
        if (StrUtil.isNotBlank(request.getChannelTradeNo())) {
            params.put("transaction_id", request.getChannelTradeNo());
        }
        if (StrUtil.isNotBlank(request.getRefundReason())) {
            params.put("refund_desc", request.getRefundReason());
        }

        long startTime = System.currentTimeMillis();
        String requestData = JSON.toJSONString(params);
        String gatewayUrl = config.getGatewayUrl() + "/secapi/pay/refund";

        try {
            if (SandboxContext.isSandboxMode() || config.getSandboxMode() == 1) {
                RefundResponse response = buildRefundResponse(request);
                saveChannelLog(null, request.getOrderNo(), "REFUND",
                        gatewayUrl, requestData, JSON.toJSONString(response),
                        response.getChannelRefundNo(), (int) (System.currentTimeMillis() - startTime), null);
                return response;
            }

            params.put("sign", SignUtil.wechatSign(params, apiKey));
            String xmlParams = mapToXml(params);
            String result = HttpUtil.postXml(gatewayUrl, xmlParams);
            Map<String, String> resultMap = xmlToMap(result);

            if (!SUCCESS_CODE.equals(resultMap.get("return_code"))) {
                saveChannelLog(null, request.getOrderNo(), "REFUND",
                        gatewayUrl, requestData, result,
                        null, (int) (System.currentTimeMillis() - startTime), resultMap.get("return_msg"));
                return RefundResponse.fail(FAIL_CODE, resultMap.getOrDefault("return_msg", "微信支付退款失败"));
            }

            if (!SUCCESS_CODE.equals(resultMap.get("result_code"))) {
                saveChannelLog(null, request.getOrderNo(), "REFUND",
                        gatewayUrl, requestData, result,
                        null, (int) (System.currentTimeMillis() - startTime), resultMap.get("err_code_des"));
                return RefundResponse.fail(resultMap.get("err_code"), resultMap.getOrDefault("err_code_des", "微信支付退款失败"));
            }

            String channelRefundNo = resultMap.get("refund_id");
            saveChannelLog(null, request.getOrderNo(), "REFUND",
                    gatewayUrl, requestData, result,
                    channelRefundNo, (int) (System.currentTimeMillis() - startTime), null);
            return RefundResponse.success(channelRefundNo, "PROCESSING");
        } catch (Exception e) {
            log.error("[微信支付]退款异常", e);
            saveChannelLog(null, request.getOrderNo(), "REFUND",
                    gatewayUrl, requestData, null,
                    null, (int) (System.currentTimeMillis() - startTime), e.getMessage());
            return RefundResponse.fail("SYSTEM_ERROR", "微信支付退款异常: " + e.getMessage());
        }
    }

    @Override
    public QueryRefundResponse queryRefund(String refundNo, String channelRefundNo) {
        log.info("[微信支付]开始查询退款, 退款单号:{}, 通道退款号:{}", refundNo, channelRefundNo);

        ChannelProperties.WechatProperties config = channelProperties.getWechat();
        Map<String, String> params = new TreeMap<>();
        params.put("appid", config.getAppId());
        params.put("mch_id", config.getMchId());
        params.put("nonce_str", IdUtil.fastSimpleUUID());
        if (StrUtil.isNotBlank(refundNo)) {
            params.put("out_refund_no", refundNo);
        }
        if (StrUtil.isNotBlank(channelRefundNo)) {
            params.put("refund_id", channelRefundNo);
        }

        long startTime = System.currentTimeMillis();
        String requestData = JSON.toJSONString(params);
        String gatewayUrl = config.getGatewayUrl() + "/pay/refundquery";

        try {
            if (SandboxContext.isSandboxMode() || config.getSandboxMode() == 1) {
                QueryRefundResponse response = buildQueryRefundResponse(refundNo, channelRefundNo, null);
                saveChannelLog(null, null, "QUERY_REFUND",
                        gatewayUrl, requestData, JSON.toJSONString(response),
                        response.getChannelRefundNo(), (int) (System.currentTimeMillis() - startTime), null);
                return response;
            }

            params.put("sign", SignUtil.wechatSign(params, config.getApiKey()));
            String xmlParams = mapToXml(params);
            String result = HttpUtil.postXml(gatewayUrl, xmlParams);
            Map<String, String> resultMap = xmlToMap(result);

            if (!SUCCESS_CODE.equals(resultMap.get("return_code"))) {
                saveChannelLog(null, null, "QUERY_REFUND",
                        gatewayUrl, requestData, result,
                        null, (int) (System.currentTimeMillis() - startTime), resultMap.get("return_msg"));
                return QueryRefundResponse.fail(FAIL_CODE, resultMap.getOrDefault("return_msg", "微信支付退款查询失败"));
            }

            String refundStatus = convertWechatRefundStatus(resultMap.get("refund_status_0"));
            BigDecimal refundAmount = new BigDecimal(resultMap.getOrDefault("refund_fee_0", "0"))
                    .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
            String refundChannelNo = resultMap.get("refund_id_0");
            LocalDateTime refundTime = LocalDateTime.now();

            saveChannelLog(null, null, "QUERY_REFUND",
                    gatewayUrl, requestData, result,
                    refundChannelNo, (int) (System.currentTimeMillis() - startTime), null);
            return QueryRefundResponse.success(refundStatus, refundAmount, refundChannelNo, refundTime);
        } catch (Exception e) {
            log.error("[微信支付]查询退款异常", e);
            saveChannelLog(null, null, "QUERY_REFUND",
                    gatewayUrl, requestData, null,
                    null, (int) (System.currentTimeMillis() - startTime), e.getMessage());
            return QueryRefundResponse.fail("SYSTEM_ERROR", "微信支付查询退款异常: " + e.getMessage());
        }
    }

    @Override
    public NotifyResult parseNotify(String notifyData, Map<String, String> params) {
        log.info("[微信支付]解析支付回调通知");

        NotifyResult result = new NotifyResult();

        if (StrUtil.isNotBlank(notifyData)) {
            try {
                Map<String, String> notifyMap = xmlToMap(notifyData);
                result.setOrderNo(notifyMap.get("out_trade_no"));
                result.setChannelTradeNo(notifyMap.get("transaction_id"));
                result.setPayStatus(convertWechatOrderStatus(notifyMap.get("result_code")));
                result.setPayAmount(new BigDecimal(notifyMap.getOrDefault("total_fee", "0"))
                        .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP));
                result.setMerchantNo(notifyMap.get("mch_id"));

                String timeEnd = notifyMap.get("time_end");
                if (StrUtil.isNotBlank(timeEnd)) {
                    result.setPayTime(LocalDateTime.parse(timeEnd, DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
                }
            } catch (Exception e) {
                log.warn("[微信支付]XML解析失败，尝试JSON解析", e);
                JSONObject jsonObject = JSON.parseObject(notifyData);
                JSONObject resource = jsonObject.getJSONObject("resource");
                if (resource != null) {
                    result.setOrderNo(resource.getString("out_trade_no"));
                    result.setChannelTradeNo(resource.getString("transaction_id"));
                    result.setPayStatus(convertWechatOrderStatus(resource.getString("trade_state")));
                    JSONObject amount = resource.getJSONObject("amount");
                    if (amount != null) {
                        result.setPayAmount(new BigDecimal(amount.getIntValue("total"))
                                .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP));
                    }
                    result.setMerchantNo(jsonObject.getString("mchid"));
                    String successTime = resource.getString("success_time");
                    if (StrUtil.isNotBlank(successTime)) {
                        result.setPayTime(LocalDateTime.parse(successTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                    }
                }
            }
        } else if (params != null && !params.isEmpty()) {
            result.setOrderNo(params.get("out_trade_no"));
            result.setChannelTradeNo(params.get("transaction_id"));
            result.setPayStatus(convertWechatOrderStatus(params.get("result_code")));
            result.setPayAmount(new BigDecimal(params.getOrDefault("total_fee", "0"))
                    .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP));
            result.setMerchantNo(params.get("mch_id"));

            String timeEnd = params.get("time_end");
            if (StrUtil.isNotBlank(timeEnd)) {
                result.setPayTime(LocalDateTime.parse(timeEnd, DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            }
        } else {
            result.setOrderNo("SANDBOX_WX_ORDER_" + IdUtil.getSnowflakeNextIdStr());
            result.setChannelTradeNo(generateChannelTradeNo());
            result.setPayStatus("SUCCESS");
            result.setPayAmount(getDefaultAmount());
            result.setPayTime(LocalDateTime.now());
            result.setMerchantNo("sandbox_wx_mch");
        }

        log.info("[微信支付]回调解析结果:{}", JSON.toJSONString(result));
        return result;
    }

    @Override
    public boolean verifyNotify(Map<String, String> params) {
        if (SandboxContext.isSandboxMode() || channelProperties.getWechat().getSandboxMode() == 1) {
            return super.verifyNotify(params);
        }
        return SignUtil.wechatVerify(params, channelProperties.getWechat().getApiKey());
    }

    private String getWechatApiPath(String payType) {
        if ("H5".equalsIgnoreCase(payType)) {
            return "/pay/unifiedorder";
        } else if ("NATIVE".equalsIgnoreCase(payType)) {
            return "/pay/unifiedorder";
        } else if ("JSAPI".equalsIgnoreCase(payType)) {
            return "/pay/unifiedorder";
        } else if ("APP".equalsIgnoreCase(payType)) {
            return "/pay/unifiedorder";
        }
        return "/pay/unifiedorder";
    }

    private String extractWechatPayParams(Map<String, String> resultMap, String payType, String appId, String apiKey) {
        if ("NATIVE".equalsIgnoreCase(payType)) {
            return resultMap.get("code_url");
        } else if ("H5".equalsIgnoreCase(payType)) {
            return resultMap.get("mweb_url");
        } else if ("JSAPI".equalsIgnoreCase(payType)) {
            Map<String, String> jsapiParams = new TreeMap<>();
            jsapiParams.put("appId", appId);
            jsapiParams.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
            jsapiParams.put("nonceStr", IdUtil.fastSimpleUUID());
            jsapiParams.put("package", "prepay_id=" + resultMap.get("prepay_id"));
            jsapiParams.put("signType", "MD5");
            jsapiParams.put("paySign", SignUtil.wechatSign(jsapiParams, apiKey));
            return JSON.toJSONString(jsapiParams);
        } else if ("APP".equalsIgnoreCase(payType)) {
            Map<String, String> appParams = new TreeMap<>();
            appParams.put("appid", appId);
            appParams.put("partnerid", resultMap.get("mch_id"));
            appParams.put("prepayid", resultMap.get("prepay_id"));
            appParams.put("package", "Sign=WXPay");
            appParams.put("noncestr", IdUtil.fastSimpleUUID());
            appParams.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            appParams.put("sign", SignUtil.wechatSign(appParams, apiKey));
            return JSON.toJSONString(appParams);
        }
        return JSON.toJSONString(resultMap);
    }

    private String convertWechatOrderStatus(String wechatStatus) {
        if (StrUtil.isBlank(wechatStatus)) {
            return "PENDING";
        }
        switch (wechatStatus) {
            case "SUCCESS":
                return "SUCCESS";
            case "REFUND":
                return "REFUNDED";
            case "NOTPAY":
                return "PENDING";
            case "CLOSED":
                return "CLOSED";
            case "REVOKED":
                return "CLOSED";
            case "USERPAYING":
                return "PENDING";
            case "PAYERROR":
                return "FAILED";
            default:
                return "PENDING";
        }
    }

    private String convertWechatRefundStatus(String wechatStatus) {
        if (StrUtil.isBlank(wechatStatus)) {
            return "PROCESSING";
        }
        switch (wechatStatus) {
            case "SUCCESS":
                return "SUCCESS";
            case "REFUNDCLOSE":
                return "FAILED";
            case "PROCESSING":
                return "PROCESSING";
            case "CHANGE":
                return "PROCESSING";
            default:
                return "PROCESSING";
        }
    }

    private String mapToXml(Map<String, String> params) {
        StringBuilder sb = new StringBuilder("<xml>");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (StrUtil.isNotBlank(entry.getKey()) && entry.getValue() != null) {
                sb.append("<").append(entry.getKey()).append(">")
                        .append("<![CDATA[").append(entry.getValue()).append("]]>")
                        .append("</").append(entry.getKey()).append(">");
            }
        }
        sb.append("</xml>");
        return sb.toString();
    }

    private Map<String, String> xmlToMap(String xml) {
        Map<String, String> result = new TreeMap<>();
        try {
            cn.hutool.core.util.XmlUtil.readAsDocument(xml).getRootElement().elements().forEach(e -> {
                result.put(e.getName(), e.getText());
            });
        } catch (Exception e) {
            log.warn("XML解析失败", e);
        }
        return result;
    }
}
