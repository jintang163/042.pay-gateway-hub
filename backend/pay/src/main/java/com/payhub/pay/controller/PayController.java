package com.payhub.pay.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.pay.dto.*;
import com.payhub.pay.dto.vo.ChannelDistributionVO;
import com.payhub.pay.dto.vo.OrderAttributionVO;
import com.payhub.pay.dto.vo.OverviewStatsVO;
import com.payhub.pay.dto.vo.SuccessRateTrendVO;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.service.DashboardService;
import com.payhub.pay.service.OrderAttributionService;
import com.payhub.pay.service.PayOrderService;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pay")
public class PayController {

    @Autowired
    private PayOrderService payOrderService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private OrderAttributionService orderAttributionService;

    @PostMapping("/unifiedorder")
    public Result<UnifiedOrderResponse> unifiedOrder(@Valid @RequestBody UnifiedOrderRequest request,
                                                   HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        if (merchantNo != null) {
            request.setMerchantNo(merchantNo);
        }
        UnifiedOrderResponse response = payOrderService.unifiedOrder(request);
        return Result.success(response);
    }

    @PostMapping("/barcode")
    public Result<BarcodePayResponse> barcodePay(@Valid @RequestBody BarcodePayRequest request,
                                                 HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        if (merchantNo != null) {
            request.setMerchantNo(merchantNo);
        }
        String clientIp = getClientIp(httpRequest);
        if (clientIp != null && request.getClientIp() == null) {
            request.setClientIp(clientIp);
        }
        BarcodePayResponse response = payOrderService.barcodePay(request);
        return Result.success(response);
    }

    @PostMapping("/facepay")
    public Result<FacePayResponse> facePay(@Valid @RequestBody FacePayRequest request,
                                           HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        if (merchantNo != null) {
            request.setMerchantNo(merchantNo);
        }
        String clientIp = getClientIp(httpRequest);
        if (clientIp != null && request.getClientIp() == null) {
            request.setClientIp(clientIp);
        }
        FacePayResponse response = payOrderService.facePay(request);
        return Result.success(response);
    }

    @GetMapping("/barcode/retry/{orderNo}")
    public Result<BarcodePayResponse> barcodeRetry(@PathVariable String orderNo,
                                                    HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
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
    public Result<FacePayResponse> facePayRetry(@PathVariable String orderNo,
                                                 HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
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

    @PostMapping("/query")
    public Result<OrderQueryResponse> queryOrder(@Valid @RequestBody OrderQueryRequest request,
                                             HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        if (merchantNo != null) {
            request.setMerchantNo(merchantNo);
        }
        OrderQueryResponse response = payOrderService.queryOrder(request);
        return Result.success(response);
    }

    @PostMapping("/notify/{channel}")
    public String notify(@PathVariable String channel, HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            params.put(name, request.getParameter(name));
        }
        StringBuilder body = new StringBuilder();
        try {
            java.io.BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        } catch (Exception e) {
            log.warn("读取通知请求体失败", e);
        }
        return payOrderService.handleNotify(channel, params, body.toString());
    }

    @GetMapping("/order/list")
    public Result<IPage<PayOrder>> orderList(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String merchantOrderNo,
            @RequestParam(required = false) Integer payStatus,
            @RequestParam(required = false) String payChannel,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        Map<String, Object> params = new HashMap<>();
        params.put("orderNo", orderNo);
        params.put("merchantOrderNo", merchantOrderNo);
        params.put("payStatus", payStatus);
        params.put("payChannel", payChannel);
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        IPage<PayOrder> page = payOrderService.listPage(current, size, merchantNo, params);
        return Result.success(page);
    }

    @GetMapping("/order/{orderNo}")
    public Result<PayOrder> orderDetail(@PathVariable String orderNo, HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        PayOrder order = payOrderService.getOrderDetail(orderNo, merchantNo);
        return Result.success(order);
    }

    @GetMapping("/order/{orderNo}/attribution")
    public Result<OrderAttributionVO> orderAttribution(@PathVariable String orderNo, HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        OrderAttributionVO vo = orderAttributionService.analyzeFailReason(orderNo, merchantNo);
        return Result.success(vo);
    }

    @GetMapping("/dashboard/overview")
    public Result<OverviewStatsVO> getOverviewStats(HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        OverviewStatsVO stats = dashboardService.getOverviewStats(merchantNo);
        return Result.success(stats);
    }

    @GetMapping("/dashboard/success-rate")
    public Result<List<SuccessRateTrendVO>> getSuccessRateTrend(
            @RequestParam(defaultValue = "7") Integer days,
            HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        List<SuccessRateTrendVO> trend = dashboardService.getSuccessRateTrend(merchantNo, days);
        return Result.success(trend);
    }

    @GetMapping("/dashboard/channel-distribution")
    public Result<List<ChannelDistributionVO>> getChannelDistribution(HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        List<ChannelDistributionVO> distribution = dashboardService.getPayChannelDistribution(merchantNo);
        return Result.success(distribution);
    }

    @GetMapping("/dashboard/recent-orders")
    public Result<List<PayOrder>> getRecentOrders(
            @RequestParam(defaultValue = "10") Integer limit,
            HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        List<PayOrder> orders = dashboardService.getRecentOrders(merchantNo, limit);
        return Result.success(orders);
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
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
