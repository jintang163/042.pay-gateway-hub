package com.payhub.pay.controller;

import cn.hutool.core.util.StrUtil;
import com.payhub.common.context.CurrentUserContext;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.Result;
import com.payhub.pay.dto.AggregateCodeOrderRequest;
import com.payhub.pay.dto.AggregateCodeOrderResponse;
import com.payhub.pay.dto.AggregateCodeQueryResponse;
import com.payhub.pay.service.AggregateCodePayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/pay/aggregate")
public class AggregateCodeController {

    @Autowired
    private AggregateCodePayService aggregateCodePayService;

    @Value("${payhub.aggregate.frontend-redirect-url:}")
    private String frontendRedirectUrl;

    @PostMapping("/order")
    public Result<AggregateCodeOrderResponse> createOrder(
            @Valid @RequestBody AggregateCodeOrderRequest request,
            HttpServletRequest httpRequest) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        if (merchantNo != null) {
            request.setMerchantNo(merchantNo);
        }
        AggregateCodeOrderResponse response = aggregateCodePayService.createOrder(request);
        return Result.success(response);
    }

    @GetMapping("/order/{orderNo}")
    public Result<AggregateCodeQueryResponse> queryOrder(@PathVariable String orderNo) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        AggregateCodeQueryResponse response = aggregateCodePayService.queryOrder(orderNo, merchantNo);
        return Result.success(response);
    }

    @GetMapping("/order/{orderNo}/qrcode")
    public Result<String> getQrCodeBase64(
            @PathVariable String orderNo,
            @RequestParam(defaultValue = "300") Integer size) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        AggregateCodeQueryResponse order = aggregateCodePayService.queryOrder(orderNo, merchantNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        String base64 = aggregateCodePayService.generateQrCodeBase64(orderNo, size);
        return Result.success(base64);
    }
}
