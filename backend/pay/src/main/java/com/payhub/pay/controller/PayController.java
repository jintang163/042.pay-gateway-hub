package com.payhub.pay.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.pay.dto.OrderQueryRequest;
import com.payhub.pay.dto.OrderQueryResponse;
import com.payhub.pay.dto.UnifiedOrderRequest;
import com.payhub.pay.dto.UnifiedOrderResponse;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.service.PayOrderService;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pay")
public class PayController {

    @Autowired
    private PayOrderService payOrderService;

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
}
