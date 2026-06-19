package com.payhub.pay.controller;

import com.alibaba.fastjson2.JSON;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.Result;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.marketing.dto.PayLinkVO;
import com.payhub.marketing.service.PayLinkService;
import com.payhub.pay.dto.UnifiedOrderRequest;
import com.payhub.pay.dto.UnifiedOrderResponse;
import com.payhub.pay.service.PayOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/public/pay-link")
public class PayLinkOrderController {

    @Autowired(required = false)
    private PayLinkService payLinkService;

    @Autowired
    private PayOrderService payOrderService;

    @GetMapping("/info/{linkCode}")
    public Result<PayLinkVO> getLinkInfo(@PathVariable String linkCode) {
        if (payLinkService == null) {
            throw new BusinessException("营销模块未启用");
        }
        PayLinkVO link = payLinkService.resolveLink(linkCode);
        return Result.success(link);
    }

    @PostMapping("/order")
    public Result<UnifiedOrderResponse> createOrder(@Valid @RequestBody PayLinkOrderRequest request,
                                                    HttpServletRequest httpRequest) {
        if (payLinkService == null) {
            throw new BusinessException("营销模块未启用");
        }

        PayLinkVO link = payLinkService.resolveLink(request.getLinkCode());

        BigDecimal payAmount;
        if (link.getFixedAmount() != null && !link.getAmountEditable()) {
            payAmount = link.getFixedAmount();
        } else if (request.getPayAmount() != null) {
            payAmount = request.getPayAmount();
            if (link.getMinAmount() != null && payAmount.compareTo(link.getMinAmount()) < 0) {
                throw new BusinessException("支付金额不能低于" + link.getMinAmount() + "元");
            }
            if (link.getMaxAmount() != null && payAmount.compareTo(link.getMaxAmount()) > 0) {
                throw new BusinessException("支付金额不能超过" + link.getMaxAmount() + "元");
            }
        } else if (link.getFixedAmount() != null) {
            payAmount = link.getFixedAmount();
        } else {
            throw new BusinessException("请输入支付金额");
        }

        String merchantOrderNo = OrderNoGenerator.generateWithPrefix("ML");

        UnifiedOrderRequest orderRequest = new UnifiedOrderRequest();
        orderRequest.setMerchantNo(link.getMerchantNo());
        orderRequest.setMerchantOrderNo(merchantOrderNo);
        orderRequest.setPayAmount(payAmount);
        orderRequest.setPayChannel(request.getPayChannel() != null ? request.getPayChannel() :
                (link.getPayChannel() != null ? link.getPayChannel() : "ALIPAY"));
        orderRequest.setPayType("NATIVE");
        orderRequest.setProductSubject(link.getProductSubject() != null ? link.getProductSubject() : link.getTitle());
        orderRequest.setProductDetail(link.getProductDetail());
        orderRequest.setNotifyUrl(link.getNotifyUrl());
        orderRequest.setLinkCode(request.getLinkCode());
        orderRequest.setCouponCode(request.getCouponCode());
        orderRequest.setActivityCode(request.getActivityCode());

        String clientIp = getClientIp(httpRequest);
        orderRequest.setClientIp(clientIp);

        if (request.getUserIdentity() != null) {
            orderRequest.setUserIdentity(request.getUserIdentity());
        }

        log.info("支付链接创建订单: linkCode={}, merchantNo={}, amount={}",
                request.getLinkCode(), link.getMerchantNo(), payAmount);

        UnifiedOrderResponse response = payOrderService.unifiedOrder(orderRequest);

        return Result.success(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @lombok.Data
    public static class PayLinkOrderRequest {
        @javax.validation.constraints.NotBlank(message = "链接编码不能为空")
        private String linkCode;

        private BigDecimal payAmount;

        private String payChannel;

        private String couponCode;

        private String activityCode;

        private String userIdentity;
    }
}
