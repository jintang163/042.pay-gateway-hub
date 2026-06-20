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

    protected static final int BARCODE_RETRY_MAX_COUNT = 10;

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
            case BARCODE:
                Map<String, String> barcodeParams = new ConcurrentHashMap<>();
                barcodeParams.put("tip", "请使用扫码枪扫描用户付款码");
                barcodeParams.put("expireTime", expireTime);
                payParams = JSON.toJSONString(barcodeParams);
                break;
            case FACEPAY:
                Map<String, String> faceParams = new ConcurrentHashMap<>();
                faceParams.put("tip", "请将面部对准摄像头完成刷脸支付");
                faceParams.put("expireTime", expireTime);
                payParams = JSON.toJSONString(faceParams);
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

    @Override
    public BarcodePayResponse barcodePay(BarcodePayRequest request) {
        log.info("[{}]开始条码支付(被扫), 订单号:{}, 金额:{}, 付款码:{}",
                getChannelCode(), request.getOrderNo(), request.getAmount(),
                maskAuthCode(request.getAuthCode()));

        long startTime = System.currentTimeMillis();
        String requestData = JSON.toJSONString(request);

        try {
            if (SandboxContext.isSandboxMode()) {
                BarcodePayResponse response = SandboxSceneSimulator.simulateBarcodePay(request,
                        () -> doBuildBarcodePayResponse(request));
                saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "BARCODE_PAY",
                        "", requestData, JSON.toJSONString(response),
                        response.getChannelTradeNo(), (int) (System.currentTimeMillis() - startTime), null);
                return response;
            }
            BarcodePayResponse response = doBuildBarcodePayResponse(request);
            saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "BARCODE_PAY",
                    "", requestData, JSON.toJSONString(response),
                    response.getChannelTradeNo(), (int) (System.currentTimeMillis() - startTime), null);
            return response;
        } catch (Exception e) {
            log.error("[{}]条码支付异常", getChannelCode(), e);
            saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "BARCODE_PAY",
                    "", requestData, null, null, (int) (System.currentTimeMillis() - startTime), e.getMessage());
            return BarcodePayResponse.fail("SYSTEM_ERROR", "条码支付异常: " + e.getMessage());
        }
    }

    private BarcodePayResponse doBuildBarcodePayResponse(BarcodePayRequest request) {
        String channelTradeNo = generateChannelTradeNo();
        String buyerUserId = "sandbox_buyer_" + IdUtil.fastSimpleUUID().substring(0, 8);
        String buyerLogonId = "user***@example.com";

        double rand = Math.random();
        if (rand < 0.75) {
            log.info("[{}]条码支付成功(沙箱), 订单号:{}, 通道交易号:{}",
                    getChannelCode(), request.getOrderNo(), channelTradeNo);
            return BarcodePayResponse.success(
                    request.getOrderNo(),
                    channelTradeNo,
                    request.getAmount(),
                    LocalDateTime.now(),
                    buyerUserId,
                    buyerLogonId
            );
        } else if (rand < 0.90) {
            log.info("[{}]条码支付用户支付中(沙箱), 订单号:{}", getChannelCode(), request.getOrderNo());
            return BarcodePayResponse.paying(request.getOrderNo(), "用户支付中，需要输入密码");
        } else {
            log.info("[{}]条码支付失败(沙箱), 订单号:{}", getChannelCode(), request.getOrderNo());
            return BarcodePayResponse.fail("PAY_FAIL", "付款码已失效，请刷新后重试");
        }
    }

    @Override
    public FacePayResponse facePay(FacePayRequest request) {
        log.info("[{}]开始刷脸支付, 订单号:{}, 金额:{}",
                getChannelCode(), request.getOrderNo(), request.getAmount());

        long startTime = System.currentTimeMillis();
        String requestData = JSON.toJSONString(request);

        try {
            if (SandboxContext.isSandboxMode()) {
                FacePayResponse response = SandboxSceneSimulator.simulateFacePay(request,
                        () -> doBuildFacePayResponse(request));
                saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "FACE_PAY",
                        "", requestData, JSON.toJSONString(response),
                        response.getChannelTradeNo(), (int) (System.currentTimeMillis() - startTime), null);
                return response;
            }
            FacePayResponse response = doBuildFacePayResponse(request);
            saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "FACE_PAY",
                    "", requestData, JSON.toJSONString(response),
                    response.getChannelTradeNo(), (int) (System.currentTimeMillis() - startTime), null);
            return response;
        } catch (Exception e) {
            log.error("[{}]刷脸支付异常", getChannelCode(), e);
            saveChannelLog(request.getMerchantNo(), request.getOrderNo(), "FACE_PAY",
                    "", requestData, null, null, (int) (System.currentTimeMillis() - startTime), e.getMessage());
            return FacePayResponse.fail("SYSTEM_ERROR", "刷脸支付异常: " + e.getMessage());
        }
    }

    private FacePayResponse doBuildFacePayResponse(FacePayRequest request) {
        String channelTradeNo = generateChannelTradeNo();
        String openId = "sandbox_openid_" + IdUtil.fastSimpleUUID().substring(0, 16);
        String buyerUserId = "sandbox_buyer_" + IdUtil.fastSimpleUUID().substring(0, 8);
        String buyerLogonId = "face***@example.com";

        double rand = Math.random();
        if (rand < 0.70) {
            log.info("[{}]刷脸支付成功(沙箱), 订单号:{}, 通道交易号:{}",
                    getChannelCode(), request.getOrderNo(), channelTradeNo);
            return FacePayResponse.success(
                    request.getOrderNo(),
                    channelTradeNo,
                    request.getAmount(),
                    LocalDateTime.now(),
                    buyerUserId,
                    buyerLogonId,
                    openId
            );
        } else if (rand < 0.85) {
            log.info("[{}]刷脸需再次验证(沙箱), 订单号:{}", getChannelCode(), request.getOrderNo());
            return FacePayResponse.needAuth("face_token_" + IdUtil.fastSimpleUUID(), "请再次刷脸验证");
        } else if (rand < 0.95) {
            log.info("[{}]刷脸支付用户确认中(沙箱), 订单号:{}", getChannelCode(), request.getOrderNo());
            return FacePayResponse.paying(request.getOrderNo(), "请在手机上确认支付");
        } else {
            log.info("[{}]刷脸支付失败(沙箱), 订单号:{}", getChannelCode(), request.getOrderNo());
            return FacePayResponse.fail("FACE_FAIL", "人脸识别未通过，请重试");
        }
    }

    private String maskAuthCode(String authCode) {
        if (StrUtil.isBlank(authCode)) {
            return "";
        }
        if (authCode.length() <= 8) {
            return authCode.charAt(0) + "****" + authCode.charAt(authCode.length() - 1);
        }
        return authCode.substring(0, 4) + "****" + authCode.substring(authCode.length() - 4);
    }
}
