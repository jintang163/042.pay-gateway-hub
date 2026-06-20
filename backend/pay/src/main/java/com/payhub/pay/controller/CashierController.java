package com.payhub.pay.controller;

import com.payhub.common.context.CurrentUserContext;
import com.payhub.common.result.Result;
import com.payhub.pay.dto.*;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.service.AggregateCodePayService;
import com.payhub.pay.service.PayOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cashier")
public class CashierController {

    @Autowired
    private PayOrderService payOrderService;

    @Autowired
    private AggregateCodePayService aggregateCodePayService;

    @GetMapping("/init")
    public Result<Map<String, Object>> initCashier() {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        Map<String, Object> data = new HashMap<>();
        data.put("merchantNo", merchantNo);
        data.put("signFree", true);
        data.put("time", System.currentTimeMillis());
        return Result.success(data);
    }

    @PostMapping("/barcode")
    public Result<BarcodePayResponse> barcodePay(@RequestBody Map<String, Object> params,
                                                 HttpServletRequest httpRequest) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        BarcodePayRequest request = new BarcodePayRequest();
        request.setMerchantNo(merchantNo);
        request.setMerchantOrderNo((String) params.get("merchantOrderNo"));
        request.setPayAmount(new BigDecimal(params.get("payAmount").toString()));
        request.setPayChannel((String) params.get("payChannel"));
        request.setAuthCode((String) params.get("authCode"));
        request.setProductSubject(params.get("productSubject") == null ? "商品购买" : (String) params.get("productSubject"));
        request.setProductDetail((String) params.get("productDetail"));
        request.setNotifyUrl(params.get("notifyUrl") == null ? "https://cashier.internal/notify" : (String) params.get("notifyUrl"));
        request.setClientIp(getClientIp(httpRequest));
        request.setScene(params.get("scene") == null ? "bar_code" : (String) params.get("scene"));
        request.setCouponCode((String) params.get("couponCode"));
        request.setActivityCode((String) params.get("activityCode"));
        BarcodePayResponse response = payOrderService.barcodePay(request);
        return Result.success(response);
    }

    @PostMapping("/facepay")
    public Result<FacePayResponse> facePay(@RequestBody Map<String, Object> params,
                                           HttpServletRequest httpRequest) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        FacePayRequest request = new FacePayRequest();
        request.setMerchantNo(merchantNo);
        request.setMerchantOrderNo((String) params.get("merchantOrderNo"));
        request.setPayAmount(new BigDecimal(params.get("payAmount").toString()));
        request.setPayChannel((String) params.get("payChannel"));
        request.setFaceCode((String) params.get("faceCode"));
        request.setOpenId((String) params.get("openId"));
        request.setSceneInfo((String) params.get("sceneInfo"));
        request.setProductSubject(params.get("productSubject") == null ? "商品购买" : (String) params.get("productSubject"));
        request.setNotifyUrl(params.get("notifyUrl") == null ? "https://cashier.internal/notify" : (String) params.get("notifyUrl"));
        request.setClientIp(getClientIp(httpRequest));
        FacePayResponse response = payOrderService.facePay(request);
        return Result.success(response);
    }

    @GetMapping("/barcode/retry/{orderNo}")
    public Result<BarcodePayResponse> barcodeRetry(@PathVariable String orderNo) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        PayOrder order = payOrderService.getOrderDetail(orderNo, merchantNo);
        OrderQueryRequest queryRequest = new OrderQueryRequest();
        queryRequest.setOrderNo(orderNo);
        queryRequest.setMerchantNo(merchantNo);
        OrderQueryResponse queryResponse = payOrderService.queryOrder(queryRequest);
        BarcodePayResponse response = BarcodePayResponse.builder()
                .orderNo(order.getOrderNo())
                .merchantOrderNo(order.getMerchantOrderNo())
                .payStatus(queryResponse.getPayStatus())
                .payAmount(queryResponse.getPayAmount())
                .payTime(queryResponse.getPayTime())
                .channelTradeNo(queryResponse.getChannelTradeNo())
                .build();
        if ("SUCCESS".equalsIgnoreCase(queryResponse.getPayStatus())) {
            response.setCode("10000");
            response.setMsg("Success");
        } else if ("PENDING".equalsIgnoreCase(queryResponse.getPayStatus())) {
            response.setPayStatus("PAYING");
            response.setSubMsg("用户支付中，请继续等待");
        } else {
            response.setCode("PAY_FAIL");
            response.setMsg("支付失败");
        }
        return Result.success(response);
    }

    @GetMapping("/facepay/retry/{orderNo}")
    public Result<FacePayResponse> facePayRetry(@PathVariable String orderNo) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        PayOrder order = payOrderService.getOrderDetail(orderNo, merchantNo);
        OrderQueryRequest queryRequest = new OrderQueryRequest();
        queryRequest.setOrderNo(orderNo);
        queryRequest.setMerchantNo(merchantNo);
        OrderQueryResponse queryResponse = payOrderService.queryOrder(queryRequest);
        FacePayResponse response = FacePayResponse.builder()
                .orderNo(order.getOrderNo())
                .merchantOrderNo(order.getMerchantOrderNo())
                .payStatus(queryResponse.getPayStatus())
                .payAmount(queryResponse.getPayAmount())
                .payTime(queryResponse.getPayTime())
                .channelTradeNo(queryResponse.getChannelTradeNo())
                .build();
        if ("SUCCESS".equalsIgnoreCase(queryResponse.getPayStatus())) {
            response.setCode("10000");
            response.setMsg("Success");
        } else if ("PENDING".equalsIgnoreCase(queryResponse.getPayStatus())) {
            response.setPayStatus("PAYING");
            response.setSubMsg("用户确认中，请继续等待");
        } else {
            response.setCode("PAY_FAIL");
            response.setMsg("支付失败");
        }
        return Result.success(response);
    }

    @PostMapping("/aggregate/order")
    public Result<AggregateCodeOrderResponse> createAggregateOrder(@RequestBody Map<String, Object> params,
                                                                   HttpServletRequest httpRequest) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        AggregateCodeOrderRequest request = new AggregateCodeOrderRequest();
        request.setMerchantNo(merchantNo);
        request.setMerchantOrderNo((String) params.get("merchantOrderNo"));
        request.setPayAmount(new BigDecimal(params.get("payAmount").toString()));
        request.setProductSubject(params.get("productSubject") == null ? "商品购买" : (String) params.get("productSubject"));
        request.setProductDetail((String) params.get("productDetail"));
        request.setNotifyUrl(params.get("notifyUrl") == null ? "https://cashier.internal/notify" : (String) params.get("notifyUrl"));
        request.setClientIp(getClientIp(httpRequest));
        request.setExpireMinutes(params.get("expireMinutes") == null ? "15" : params.get("expireMinutes").toString());
        AggregateCodeOrderResponse response = aggregateCodePayService.createOrder(request);
        return Result.success(response);
    }

    @GetMapping("/aggregate/order/{orderNo}")
    public Result<AggregateCodeOrderResponse> getAggregateOrder(@PathVariable String orderNo) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        var queryResp = aggregateCodePayService.queryOrder(orderNo, merchantNo);
        AggregateCodeOrderResponse response = AggregateCodeOrderResponse.builder()
                .orderNo(queryResp.getOrderNo())
                .merchantOrderNo(queryResp.getMerchantOrderNo())
                .payAmount(queryResp.getPayAmount())
                .payStatus(queryResp.getPayStatus())
                .payChannel(queryResp.getPayChannel())
                .qrCodeUrl(queryResp.getQrCodeUrl())
                .build();
        return Result.success(response);
    }

    @GetMapping("/aggregate/order/{orderNo}/qrcode")
    public Result<String> getAggregateQrCode(@PathVariable String orderNo,
                                             @RequestParam(defaultValue = "200") int size) {
        String qrBase64 = aggregateCodePayService.generateQrCodeBase64(orderNo, size);
        return Result.success(qrBase64);
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }
}
