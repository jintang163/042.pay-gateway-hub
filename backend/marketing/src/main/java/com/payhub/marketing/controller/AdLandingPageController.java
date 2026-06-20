package com.payhub.marketing.controller;

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
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/ad")
public class AdLandingPageController {

    @Autowired
    private MerchantAdService merchantAdService;

    @GetMapping("/success/{merchantNo}")
    public String paySuccessAd(@PathVariable String merchantNo,
                               @RequestParam(required = false) String orderNo,
                               @RequestParam(defaultValue = "0.00") String amount,
                               @RequestParam(defaultValue = "PAY_SUCCESS") String position,
                               @RequestParam(defaultValue = "5") Integer limit,
                               Model model,
                               HttpServletRequest request) {
        try {
            List<AdDisplayVO> ads = merchantAdService.listDisplayAds(merchantNo, position, limit);
            model.addAttribute("ads", ads);
            model.addAttribute("merchantNo", merchantNo);
            model.addAttribute("orderNo", orderNo);
            model.addAttribute("payAmount", amount);
            model.addAttribute("position", position);
            model.addAttribute("clientIp", getClientIp(request));
            log.info("支付成功广告页访问, merchantNo={}, orderNo={}, adCount={}", merchantNo, orderNo, ads.size());
        } catch (Exception e) {
            log.error("加载支付成功广告页异常", e);
            model.addAttribute("ads", java.util.Collections.emptyList());
        }
        return "ad-success";
    }

    @GetMapping("/click/redirect")
    public String redirectAd(@RequestParam String adCode,
                             @RequestParam(required = false) String merchantNo,
                             @RequestParam(required = false) String orderNo,
                             @RequestParam(required = false) String position,
                             @RequestParam(defaultValue = "0.00") String payAmount,
                             HttpServletRequest request) {
        try {
            com.payhub.marketing.dto.AdClickReportRequest clickReq = new com.payhub.marketing.dto.AdClickReportRequest();
            clickReq.setAdCode(adCode);
            clickReq.setOrderNo(orderNo);
            clickReq.setPosition(position);
            clickReq.setPayAmount(new java.math.BigDecimal(payAmount));
            clickReq.setRefererUrl(request.getHeader("Referer"));
            clickReq.setDeviceId(request.getHeader("User-Agent") != null
                    ? request.getHeader("User-Agent").hashCode() + "" : null);
            com.payhub.marketing.dto.AdClickReportResult result =
                    merchantAdService.reportClick(clickReq, request);
            if (result.getTargetUrl() != null && !result.getTargetUrl().isEmpty()) {
                return "redirect:" + result.getTargetUrl();
            }
        } catch (Exception e) {
            log.error("广告点击跳转异常, adCode={}", adCode, e);
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
