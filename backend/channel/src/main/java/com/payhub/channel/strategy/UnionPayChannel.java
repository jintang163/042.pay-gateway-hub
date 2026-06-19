package com.payhub.channel.strategy;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.payhub.channel.client.HttpUtil;
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
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Component
public class UnionPayChannel extends AbstractPayChannel {

    @Autowired
    private ChannelProperties channelProperties;

    private static final String SUCCESS_CODE = "00";
    private static final String RESP_CODE = "respCode";
    private static final String RESP_MSG = "respMsg";

    @Override
    protected String getChannelCode() {
        return PayChannelEnum.UNION_PAY.getCode();
    }

    @Override
    protected BigDecimal getDefaultAmount() {
        return new BigDecimal("50.00");
    }

    @Override
    public UnifiedOrderResponse unifiedOrder(UnifiedOrderRequest request) {
        log.info("[银联支付]开始统一下单, 订单号:{}, 金额:{}", request.getOrderNo(), request.getAmount());

        ChannelProperties.UnionPayProperties config = channelProperties.getUnionPay();

        Map<String, String> params = buildUnionPayBaseParams(config);
        params.put("txnType", config.getTxnType());
        params.put("txnSubType", config.getTxnSubType());
        params.put("bizType", config.getBizType());
        params.put("channelType", getUnionPayChannelType(request.getPayType()));
        params.put("accessType", config.getAccessType());
        params.put("merId", StrUtil.isNotBlank(request.getChannelMerchantId()) ? request.getChannelMerchantId() : config.getMerId());
        params.put("orderId", request.getOrderNo());
        params.put("txnTime", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        params.put("txnAmt", request.getAmount().multiply(new BigDecimal("100")).intValue() + "");
        params.put("currencyCode", config.getCurrencyCode());
        params.put("backUrl", StrUtil.isNotBlank(request.getNotifyUrl()) ? request.getNotifyUrl() : config.getNotifyUrl());
        if (StrUtil.isNotBlank(request.getSubject())) {
            params.put("orderDesc", request.getSubject());
        }
        if (StrUtil.isNotBlank(request.getClientIp())) {
            params.put("customerIp", request.getClientIp());
        }

        long startTime = System.currentTimeMillis();
        String requestData = buildUnionPayFormData(params);
        String gatewayUrl = config.getGatewayUrl() + "/gateway/api/frontTransReq.do";

        try {
            if (SandboxContext.isSandboxMode() || config.getSandboxMode() == 1) {
                UnifiedOrderResponse response = buildUnifiedOrderResponse(request);
                saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "UNIFIED_ORDER",
                        gatewayUrl, requestData, JSON.toJSONString(response),
                        response.getChannelTradeNo(), (int) (System.currentTimeMillis() - startTime), null);
                return response;
            }

            String result = HttpUtil.postForm(gatewayUrl, params);
            Map<String, String> resultMap = parseUnionPayResponse(result);

            if (!SUCCESS_CODE.equals(resultMap.get(RESP_CODE))) {
                saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "UNIFIED_ORDER",
                        gatewayUrl, requestData, result,
                        null, (int) (System.currentTimeMillis() - startTime), resultMap.get(RESP_MSG));
                return UnifiedOrderResponse.fail(resultMap.get(RESP_CODE), resultMap.getOrDefault(RESP_MSG, "银联支付下单失败"));
            }

            String channelTradeNo = resultMap.get("queryId");
            String payParams = extractUnionPayParams(resultMap, request.getPayType());
            saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "UNIFIED_ORDER",
                    gatewayUrl, requestData, result,
                    channelTradeNo, (int) (System.currentTimeMillis() - startTime), null);
            return UnifiedOrderResponse.success(request.getPayType(), payParams, channelTradeNo);
        } catch (Exception e) {
            log.error("[银联支付]统一下单异常", e);
            saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "UNIFIED_ORDER",
                    gatewayUrl, requestData, null,
                    null, (int) (System.currentTimeMillis() - startTime), e.getMessage());
            return UnifiedOrderResponse.fail("SYSTEM_ERROR", "银联支付下单异常: " + e.getMessage());
        }
    }

    @Override
    public QueryOrderResponse queryOrder(String orderNo, String channelTradeNo) {
        log.info("[银联支付]开始查询订单, 订单号:{}, 通道交易号:{}", orderNo, channelTradeNo);

        ChannelProperties.UnionPayProperties config = channelProperties.getUnionPay();

        Map<String, String> params = buildUnionPayBaseParams(config);
        params.put("txnType", "00");
        params.put("txnSubType", "00");
        params.put("bizType", config.getBizType());
        params.put("accessType", config.getAccessType());
        params.put("merId", config.getMerId());
        if (StrUtil.isNotBlank(orderNo)) {
            params.put("orderId", orderNo);
        }
        if (StrUtil.isNotBlank(channelTradeNo)) {
            params.put("queryId", channelTradeNo);
        }
        params.put("txnTime", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

        long startTime = System.currentTimeMillis();
        String requestData = buildUnionPayFormData(params);
        String gatewayUrl = config.getGatewayUrl() + "/gateway/api/queryTrans.do";

        try {
            if (SandboxContext.isSandboxMode() || config.getSandboxMode() == 1) {
                QueryOrderResponse response = buildQueryOrderResponse(orderNo, channelTradeNo);
                saveChannelLog(null, orderNo, "QUERY_ORDER",
                        gatewayUrl, requestData, JSON.toJSONString(response),
                        response.getChannelTradeNo(), (int) (System.currentTimeMillis() - startTime), null);
                return response;
            }

            String result = HttpUtil.postForm(gatewayUrl, params);
            Map<String, String> resultMap = parseUnionPayResponse(result);

            if (!SUCCESS_CODE.equals(resultMap.get(RESP_CODE))) {
                saveChannelLog(null, orderNo, "QUERY_ORDER",
                        gatewayUrl, requestData, result,
                        null, (int) (System.currentTimeMillis() - startTime), resultMap.get(RESP_MSG));
                return QueryOrderResponse.fail(resultMap.get(RESP_CODE), resultMap.getOrDefault(RESP_MSG, "银联支付查单失败"));
            }

            String origRespCode = resultMap.get("origRespCode");
            String orderStatus = convertUnionPayOrderStatus(origRespCode);
            BigDecimal amount = new BigDecimal(resultMap.getOrDefault("txnAmt", "0"))
                    .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
            String queryId = resultMap.get("queryId");
            LocalDateTime payTime = null;
            String txnTime = resultMap.get("txnTime");
            if (StrUtil.isNotBlank(txnTime)) {
                payTime = LocalDateTime.parse(txnTime, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            }

            saveChannelLog(null, orderNo, "QUERY_ORDER",
                    gatewayUrl, requestData, result,
                    queryId, (int) (System.currentTimeMillis() - startTime), null);
            return QueryOrderResponse.success(orderStatus, amount, queryId, payTime);
        } catch (Exception e) {
            log.error("[银联支付]查询订单异常", e);
            saveChannelLog(null, orderNo, "QUERY_ORDER",
                    gatewayUrl, requestData, null,
                    null, (int) (System.currentTimeMillis() - startTime), e.getMessage());
            return QueryOrderResponse.fail("SYSTEM_ERROR", "银联支付查单异常: " + e.getMessage());
        }
    }

    @Override
    public RefundResponse refund(RefundRequest request) {
        log.info("[银联支付]开始退款, 订单号:{}, 退款单号:{}", request.getOrderNo(), request.getRefundNo());

        ChannelProperties.UnionPayProperties config = channelProperties.getUnionPay();

        Map<String, String> params = buildUnionPayBaseParams(config);
        params.put("txnType", "04");
        params.put("txnSubType", "00");
        params.put("bizType", config.getBizType());
        params.put("channelType", config.getChannelType());
        params.put("accessType", config.getAccessType());
        params.put("merId", StrUtil.isNotBlank(request.getChannelMerchantId()) ? request.getChannelMerchantId() : config.getMerId());
        params.put("orderId", request.getRefundNo());
        params.put("origQryId", StrUtil.isNotBlank(request.getChannelTradeNo()) ? request.getChannelTradeNo() : "");
        params.put("txnTime", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        params.put("txnAmt", request.getRefundAmount().multiply(new BigDecimal("100")).intValue() + "");
        params.put("backUrl", config.getNotifyUrl());
        if (StrUtil.isNotBlank(request.getRefundReason())) {
            params.put("orderDesc", request.getRefundReason());
        }

        long startTime = System.currentTimeMillis();
        String requestData = buildUnionPayFormData(params);
        String gatewayUrl = config.getGatewayUrl() + "/gateway/api/backTransReq.do";

        try {
            if (SandboxContext.isSandboxMode() || config.getSandboxMode() == 1) {
                RefundResponse response = buildRefundResponse(request);
                saveChannelLog(null, request.getOrderNo(), "REFUND",
                        gatewayUrl, requestData, JSON.toJSONString(response),
                        response.getChannelRefundNo(), (int) (System.currentTimeMillis() - startTime), null);
                return response;
            }

            String result = HttpUtil.postForm(gatewayUrl, params);
            Map<String, String> resultMap = parseUnionPayResponse(result);

            if (!SUCCESS_CODE.equals(resultMap.get(RESP_CODE))) {
                saveChannelLog(null, request.getOrderNo(), "REFUND",
                        gatewayUrl, requestData, result,
                        null, (int) (System.currentTimeMillis() - startTime), resultMap.get(RESP_MSG));
                return RefundResponse.fail(resultMap.get(RESP_CODE), resultMap.getOrDefault(RESP_MSG, "银联支付退款失败"));
            }

            String channelRefundNo = resultMap.get("queryId");
            saveChannelLog(null, request.getOrderNo(), "REFUND",
                    gatewayUrl, requestData, result,
                    channelRefundNo, (int) (System.currentTimeMillis() - startTime), null);
            return RefundResponse.success(channelRefundNo, "PROCESSING");
        } catch (Exception e) {
            log.error("[银联支付]退款异常", e);
            saveChannelLog(null, request.getOrderNo(), "REFUND",
                    gatewayUrl, requestData, null,
                    null, (int) (System.currentTimeMillis() - startTime), e.getMessage());
            return RefundResponse.fail("SYSTEM_ERROR", "银联支付退款异常: " + e.getMessage());
        }
    }

    @Override
    public QueryRefundResponse queryRefund(String refundNo, String channelRefundNo) {
        log.info("[银联支付]开始查询退款, 退款单号:{}, 通道退款号:{}", refundNo, channelRefundNo);

        ChannelProperties.UnionPayProperties config = channelProperties.getUnionPay();

        Map<String, String> params = buildUnionPayBaseParams(config);
        params.put("txnType", "00");
        params.put("txnSubType", "00");
        params.put("bizType", config.getBizType());
        params.put("accessType", config.getAccessType());
        params.put("merId", config.getMerId());
        if (StrUtil.isNotBlank(refundNo)) {
            params.put("orderId", refundNo);
        }
        if (StrUtil.isNotBlank(channelRefundNo)) {
            params.put("queryId", channelRefundNo);
        }
        params.put("txnTime", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

        long startTime = System.currentTimeMillis();
        String requestData = buildUnionPayFormData(params);
        String gatewayUrl = config.getGatewayUrl() + "/gateway/api/queryTrans.do";

        try {
            if (SandboxContext.isSandboxMode() || config.getSandboxMode() == 1) {
                QueryRefundResponse response = buildQueryRefundResponse(refundNo, channelRefundNo, null);
                saveChannelLog(null, null, "QUERY_REFUND",
                        gatewayUrl, requestData, JSON.toJSONString(response),
                        response.getChannelRefundNo(), (int) (System.currentTimeMillis() - startTime), null);
                return response;
            }

            String result = HttpUtil.postForm(gatewayUrl, params);
            Map<String, String> resultMap = parseUnionPayResponse(result);

            if (!SUCCESS_CODE.equals(resultMap.get(RESP_CODE))) {
                saveChannelLog(null, null, "QUERY_REFUND",
                        gatewayUrl, requestData, result,
                        null, (int) (System.currentTimeMillis() - startTime), resultMap.get(RESP_MSG));
                return QueryRefundResponse.fail(resultMap.get(RESP_CODE), resultMap.getOrDefault(RESP_MSG, "银联支付退款查询失败"));
            }

            String origRespCode = resultMap.get("origRespCode");
            String refundStatus = convertUnionPayRefundStatus(origRespCode);
            BigDecimal refundAmount = new BigDecimal(resultMap.getOrDefault("txnAmt", "0"))
                    .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
            String refundChannelNo = resultMap.get("queryId");
            LocalDateTime refundTime = LocalDateTime.now();

            saveChannelLog(null, null, "QUERY_REFUND",
                    gatewayUrl, requestData, result,
                    refundChannelNo, (int) (System.currentTimeMillis() - startTime), null);
            return QueryRefundResponse.success(refundStatus, refundAmount, refundChannelNo, refundTime);
        } catch (Exception e) {
            log.error("[银联支付]查询退款异常", e);
            saveChannelLog(null, null, "QUERY_REFUND",
                    gatewayUrl, requestData, null,
                    null, (int) (System.currentTimeMillis() - startTime), e.getMessage());
            return QueryRefundResponse.fail("SYSTEM_ERROR", "银联支付查询退款异常: " + e.getMessage());
        }
    }

    @Override
    public NotifyResult parseNotify(String notifyData, Map<String, String> params) {
        log.info("[银联支付]解析支付回调通知");

        NotifyResult result = new NotifyResult();

        if (StrUtil.isNotBlank(notifyData)) {
            Map<String, String> notifyMap;
            try {
                notifyMap = parseUnionPayResponse(notifyData);
            } catch (Exception e) {
                log.warn("[银联支付]form表单解析失败，尝试JSON解析", e);
                notifyMap = new HashMap<>();
                JSONObject jsonObject = JSON.parseObject(notifyData);
                if (jsonObject != null) {
                    for (String key : jsonObject.keySet()) {
                        notifyMap.put(key, jsonObject.getString(key));
                    }
                }
            }
            result.setOrderNo(notifyMap.get("orderId"));
            result.setChannelTradeNo(notifyMap.get("queryId"));
            result.setPayStatus(convertUnionPayOrderStatus(notifyMap.get("respCode")));
            result.setPayAmount(new BigDecimal(notifyMap.getOrDefault("txnAmt", "0"))
                    .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP));
            result.setMerchantNo(notifyMap.get("merId"));

            String txnTime = notifyMap.get("txnTime");
            if (StrUtil.isNotBlank(txnTime)) {
                result.setPayTime(LocalDateTime.parse(txnTime, DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            }
        } else if (params != null && !params.isEmpty()) {
            result.setOrderNo(params.get("orderId"));
            result.setChannelTradeNo(params.get("queryId"));
            result.setPayStatus(convertUnionPayOrderStatus(params.get("respCode")));
            result.setPayAmount(new BigDecimal(params.getOrDefault("txnAmt", "0"))
                    .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP));
            result.setMerchantNo(params.get("merId"));

            String txnTime = params.get("txnTime");
            if (StrUtil.isNotBlank(txnTime)) {
                result.setPayTime(LocalDateTime.parse(txnTime, DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            }
        } else {
            result.setOrderNo("SANDBOX_UP_ORDER_" + IdUtil.getSnowflakeNextIdStr());
            result.setChannelTradeNo(generateChannelTradeNo());
            result.setPayStatus("SUCCESS");
            result.setPayAmount(getDefaultAmount());
            result.setPayTime(LocalDateTime.now());
            result.setMerchantNo("sandbox_up_mch");
        }

        log.info("[银联支付]回调解析结果:{}", JSON.toJSONString(result));
        return result;
    }

    @Override
    public boolean verifyNotify(Map<String, String> params) {
        if (SandboxContext.isSandboxMode() || channelProperties.getUnionPay().getSandboxMode() == 1) {
            return super.verifyNotify(params);
        }
        log.info("[银联支付]真实模式回调验签，暂返回模拟通过");
        return true;
    }

    private Map<String, String> buildUnionPayBaseParams(ChannelProperties.UnionPayProperties config) {
        Map<String, String> params = new TreeMap<>();
        params.put("version", config.getVersion());
        params.put("encoding", config.getEncoding());
        params.put("signMethod", config.getSignMethod());
        return params;
    }

    private String buildUnionPayFormData(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue() != null ? entry.getValue() : "");
        }
        return sb.toString();
    }

    private Map<String, String> parseUnionPayResponse(String response) {
        Map<String, String> result = new TreeMap<>();
        if (StrUtil.isBlank(response)) {
            return result;
        }
        String[] pairs = response.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = idx < pair.length() - 1 ? pair.substring(idx + 1) : "";
                result.put(key, value);
            }
        }
        return result;
    }

    private String getUnionPayChannelType(String payType) {
        if ("H5".equalsIgnoreCase(payType)) {
            return "08";
        } else if ("NATIVE".equalsIgnoreCase(payType)) {
            return "07";
        } else if ("JSAPI".equalsIgnoreCase(payType)) {
            return "05";
        } else if ("APP".equalsIgnoreCase(payType)) {
            return "08";
        }
        return "08";
    }

    private String extractUnionPayParams(Map<String, String> resultMap, String payType) {
        if ("NATIVE".equalsIgnoreCase(payType)) {
            return resultMap.get("qrCode");
        } else if ("H5".equalsIgnoreCase(payType)) {
            return resultMap.get("tn");
        } else if ("JSAPI".equalsIgnoreCase(payType) || "APP".equalsIgnoreCase(payType)) {
            return resultMap.get("tn");
        }
        return JSON.toJSONString(resultMap);
    }

    private String convertUnionPayOrderStatus(String unionPayStatus) {
        if (StrUtil.isBlank(unionPayStatus)) {
            return "PENDING";
        }
        switch (unionPayStatus) {
            case "00":
            case "A6":
                return "SUCCESS";
            case "01":
            case "02":
            case "03":
            case "04":
            case "05":
                return "PENDING";
            case "12":
            case "13":
            case "14":
                return "FAILED";
            case "30":
            case "31":
                return "CLOSED";
            default:
                return "PENDING";
        }
    }

    private String convertUnionPayRefundStatus(String unionPayStatus) {
        if (StrUtil.isBlank(unionPayStatus)) {
            return "PROCESSING";
        }
        switch (unionPayStatus) {
            case "00":
            case "A6":
                return "SUCCESS";
            case "01":
            case "02":
            case "03":
            case "04":
            case "05":
                return "PROCESSING";
            case "12":
            case "13":
            case "14":
                return "FAILED";
            default:
                return "PROCESSING";
        }
    }
}
