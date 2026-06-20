package com.payhub.marketing.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payhub.marketing.dto.AdClickReportRequest;
import com.payhub.marketing.dto.AdClickReportResult;
import com.payhub.marketing.dto.AdDisplayVO;
import com.payhub.marketing.service.MerchantAdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/ad")
public class AdLandingPageController {

    @Autowired
    private MerchantAdService merchantAdService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @GetMapping("/success/{merchantNo}")
    public String paySuccessAd(@PathVariable String merchantNo,
                               @RequestParam(required = false) String orderNo,
                               @RequestParam(defaultValue = "0.00") String amount,
                               @RequestParam(defaultValue = "PAY_SUCCESS") String position,
                               @RequestParam(defaultValue = "5") Integer limit,
                               Model model,
                               HttpServletRequest request) {
        List<AdDisplayVO> ads = Collections.emptyList();
        List<String> adCodes = Collections.emptyList();
        try {
            ads = merchantAdService.listDisplayAds(merchantNo, position, limit);
            if (ads != null && !ads.isEmpty()) {
                adCodes = ads.stream()
                        .map(AdDisplayVO::getAdCode)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());
                try {
                    merchantAdService.recordImpression(adCodes);
                    log.info("支付成功广告页曝光记录, merchantNo={}, orderNo={}, adCodes={}", merchantNo, orderNo, adCodes);
                } catch (Exception e) {
                    log.warn("广告曝光记录失败", e);
                }
            }
        } catch (Exception e) {
            log.error("加载支付成功广告页异常", e);
        }
        model.addAttribute("ads", ads);
        model.addAttribute("merchantNo", merchantNo);
        model.addAttribute("orderNo", orderNo);
        model.addAttribute("payAmount", amount);
        model.addAttribute("position", position);
        try {
            model.addAttribute("adCodesJson", OBJECT_MAPPER.writeValueAsString(adCodes));
        } catch (JsonProcessingException e) {
            model.addAttribute("adCodesJson", "[]");
        }
        model.addAttribute("clientIp", getClientIp(request));
        return "ad-success";
    }

    @GetMapping("/click/redirect")
    public String redirectAd(@RequestParam String adCode,
                             @RequestParam(required = false) String merchantNo,
                             @RequestParam(required = false) String orderNo,
                             @RequestParam(required = false) String position,
                             @RequestParam(defaultValue = "0.00") String payAmount,
                             HttpServletRequest request) {
        String targetUrl = null;
        try {
            AdClickReportRequest clickReq = new AdClickReportRequest();
            clickReq.setAdCode(adCode);
            clickReq.setMerchantNo(merchantNo);
            clickReq.setOrderNo(orderNo);
            clickReq.setPosition(position == null ? "PAY_SUCCESS" : position);
            clickReq.setPayAmount(new BigDecimal(payAmount == null || payAmount.isEmpty() ? "0" : payAmount));
            clickReq.setRefererUrl(request.getHeader("Referer"));
            String ua = request.getHeader("User-Agent");
            clickReq.setDeviceId(ua != null ? String.valueOf(ua.hashCode()) : null);
            clickReq.setClientIp(getClientIp(request));
            AdClickReportResult result = merchantAdService.reportClick(clickReq, request);
            targetUrl = result.getTargetUrl();
            log.info("广告点击跳转, adCode={}, orderNo={}, merchantNo={}, position={}, valid={}, target={}",
                    adCode, orderNo, merchantNo, position, result.getValidClick(), targetUrl);
        } catch (Exception e) {
            log.error("广告点击跳转异常, adCode={}", adCode, e);
        }
        if (targetUrl != null && !targetUrl.isEmpty()) {
            return "redirect:" + targetUrl;
        }
        return "redirect:/";
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String[] headers = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "X-Real-IP"};
        for (String h : headers) {
            String ip = request.getHeader(h);
            if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
            }
        }
        return request.getRemoteAddr();
    }
}
