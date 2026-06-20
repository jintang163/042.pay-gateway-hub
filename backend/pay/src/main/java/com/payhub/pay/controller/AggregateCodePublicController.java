package com.payhub.pay.controller;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.payhub.common.exception.BusinessException;
import com.payhub.pay.dto.AggregateCodeOrderResponse;
import com.payhub.pay.service.AggregateCodePayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/api/public/aggregate")
public class AggregateCodePublicController {

    @Autowired
    private AggregateCodePayService aggregateCodePayService;

    @Value("${payhub.aggregate.frontend-redirect-url:}")
    private String frontendRedirectUrl;

    @Value("${payhub.aggregate.show-channel-select:true}")
    private boolean showChannelSelect;

    @GetMapping(value = "/qr/{orderNo}", produces = MediaType.TEXT_HTML_VALUE)
    public String qrCodePage(@PathVariable String orderNo,
                             @RequestParam(required = false) String channel,
                             HttpServletRequest request) {
        log.info("聚合码扫码访问: orderNo={}, channel={}, UA={}",
                orderNo, channel, request.getHeader("User-Agent"));

        String recognizedChannel = aggregateCodePayService.recognizeChannel(request);
        String targetChannel = StrUtil.isNotBlank(channel) ? channel : recognizedChannel;

        if (StrUtil.isBlank(targetChannel)) {
            if (showChannelSelect) {
                return "redirect:/api/public/aggregate/qr/" + orderNo + "/select";
            }
            targetChannel = "ALIPAY";
        }

        AggregateCodeOrderResponse channelOrder;
        try {
            String payType = resolvePayType(targetChannel, request);
            channelOrder = aggregateCodePayService.getOrCreateChannelOrder(orderNo, targetChannel, payType, request);
        } catch (Exception e) {
            log.error("创建渠道订单失败: orderNo={}, channel={}", orderNo, targetChannel, e);
            request.setAttribute("errorMsg", e.getMessage());
            return "aggregate-error";
        }

        request.setAttribute("orderNo", orderNo);
        request.setAttribute("channel", targetChannel);
        request.setAttribute("payParams", channelOrder.getPayParams());
        request.setAttribute("amount", channelOrder.getPayAmount());

        if ("WECHAT_PAY".equals(targetChannel)) {
            return "aggregate-wechat";
        } else if ("ALIPAY".equals(targetChannel)) {
            return "aggregate-alipay";
        } else if ("UNION_PAY".equals(targetChannel)) {
            return "aggregate-unionpay";
        }

        return "aggregate-index";
    }

    @GetMapping(value = "/qr/{orderNo}/select", produces = MediaType.TEXT_HTML_VALUE)
    public String channelSelectPage(@PathVariable String orderNo, HttpServletRequest request) {
        request.setAttribute("orderNo", orderNo);
        request.setAttribute("baseUrl", "/api/public/aggregate/qr/" + orderNo);
        return "aggregate-select";
    }

    @GetMapping(value = "/qr/{orderNo}/channel", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getChannelOrder(
            @PathVariable String orderNo,
            @RequestParam(required = false) String channel,
            @RequestParam(defaultValue = "NATIVE") String payType,
            HttpServletRequest request) {
        try {
            AggregateCodeOrderResponse response =
                    aggregateCodePayService.getOrCreateChannelOrder(orderNo, channel, payType, request);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("获取渠道订单失败: orderNo={}", orderNo, e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return JSON.toJSONString(result);
        }
    }

    @GetMapping(value = "/qr/{orderNo}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String getOrderStatus(@PathVariable String orderNo) {
        try {
            var resp = aggregateCodePayService.queryOrder(orderNo, null);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", resp);
            return JSON.toJSONString(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return JSON.toJSONString(result);
        }
    }

    private String resolvePayType(String channel, HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (StrUtil.isBlank(ua)) {
            return "NATIVE";
        }
        String lowerUa = ua.toLowerCase();
        if ("WECHAT_PAY".equals(channel) && lowerUa.contains("micromessenger")) {
            return "JSAPI";
        }
        if ("ALIPAY".equals(channel) && (lowerUa.contains("alipay") || lowerUa.contains("alipayclient"))) {
            return "JSAPI";
        }
        return "NATIVE";
    }
}
