package com.payhub.marketing.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.context.CurrentUserContext;
import com.payhub.common.result.Result;
import com.payhub.marketing.dto.*;
import com.payhub.marketing.service.MerchantAdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ad")
public class MerchantAdController {

    @Autowired
    private MerchantAdService merchantAdService;

    @GetMapping("/list")
    public Result<IPage<MerchantAdVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String adCode,
            @RequestParam(required = false) String adTitle,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) Integer status,
            HttpServletRequest httpRequest) {
        String merchantNo = getCurrentMerchantNo(httpRequest);
        Map<String, Object> params = new HashMap<>();
        params.put("adCode", adCode);
        params.put("adTitle", adTitle);
        params.put("position", position);
        params.put("status", status);
        IPage<MerchantAdVO> page = merchantAdService.listPage(current, size, merchantNo, params);
        return Result.success(page);
    }

    @GetMapping("/{adCode}")
    public Result<MerchantAdVO> getByAdCode(@PathVariable String adCode) {
        return Result.success(merchantAdService.getByAdCode(adCode));
    }

    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody MerchantAdSaveRequest request,
                             HttpServletRequest httpRequest) {
        String merchantNo = getCurrentMerchantNo(httpRequest);
        if (merchantNo != null && request.getMerchantNo() == null) {
            request.setMerchantNo(merchantNo);
        }
        merchantAdService.saveAd(request);
        return Result.success();
    }

    @PostMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable Long id) {
        merchantAdService.toggleStatus(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        merchantAdService.deleteAd(id);
        return Result.success();
    }

    @GetMapping("/display")
    public Result<List<AdDisplayVO>> listDisplay(
            @RequestParam String merchantNo,
            @RequestParam(defaultValue = "PAY_SUCCESS") String position,
            @RequestParam(defaultValue = "5") Integer limit,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String payAmount,
            @RequestParam(required = false) String adCodes,
            HttpServletRequest httpRequest) {
        List<AdDisplayVO> ads = merchantAdService.listDisplayAds(merchantNo, position, limit);
        if (!ads.isEmpty()) {
            List<String> codes = ads.stream().map(AdDisplayVO::getAdCode).toList();
            AdImpressionReportRequest impReq = new AdImpressionReportRequest();
            impReq.setCodes(codes);
            impReq.setMerchantNo(merchantNo);
            impReq.setOrderNo(orderNo);
            impReq.setPosition(position);
            impReq.setPayAmount(payAmount != null && !payAmount.isEmpty() ? new java.math.BigDecimal(payAmount) : java.math.BigDecimal.ZERO);
            impReq.setClientIp(getClientIp(httpRequest));
            String ua = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;
            impReq.setDeviceId(ua != null ? String.valueOf(ua.hashCode()) : null);
            impReq.setRefererUrl(httpRequest != null ? httpRequest.getHeader("Referer") : null);
            merchantAdService.recordImpression(impReq);
        }
        return Result.success(ads);
    }

    @PostMapping("/click")
    public Result<AdClickReportResult> reportClick(@Valid @RequestBody AdClickReportRequest request,
                                                   HttpServletRequest httpRequest) {
        String ua = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;
        if (ua != null && request.getDeviceId() == null) {
            request.setDeviceId(String.valueOf(ua.hashCode()));
        }
        if (request.getRefererUrl() == null && httpRequest != null) {
            request.setRefererUrl(httpRequest.getHeader("Referer"));
        }
        if (request.getClientIp() == null) {
            request.setClientIp(getClientIp(httpRequest));
        }
        if (request.getPosition() == null) {
            request.setPosition("PAY_SUCCESS");
        }
        AdClickReportResult result = merchantAdService.reportClick(request, httpRequest);
        return Result.success(result);
    }

    @PostMapping("/impression")
    public Result<Void> reportImpression(@RequestBody AdImpressionReportRequest request,
                                         HttpServletRequest httpRequest) {
        if (request == null) {
            return Result.success();
        }
        String ua = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;
        if (ua != null && request.getDeviceId() == null) {
            request.setDeviceId(String.valueOf(ua.hashCode()));
        }
        if (request.getRefererUrl() == null && httpRequest != null) {
            request.setRefererUrl(httpRequest.getHeader("Referer"));
        }
        if (request.getClientIp() == null) {
            request.setClientIp(getClientIp(httpRequest));
        }
        if (request.getPosition() == null) {
            request.setPosition("PAY_SUCCESS");
        }
        merchantAdService.recordImpression(request);
        return Result.success();
    }

    @GetMapping("/stats/overview")
    public Result<AdStatsOverviewVO> statsOverview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String adCode,
            @RequestParam(required = false) String position,
            HttpServletRequest httpRequest) {
        String merchantNo = getCurrentMerchantNo(httpRequest);
        AdStatsOverviewVO vo = merchantAdService.getStatsOverview(merchantNo, startDate, endDate, adCode, position);
        return Result.success(vo);
    }

    private String getCurrentMerchantNo(HttpServletRequest httpRequest) {
        String merchantNo = null;
        try {
            merchantNo = CurrentUserContext.getCurrentMerchantNo();
        } catch (Exception ignore) {
        }
        if (merchantNo == null && httpRequest != null) {
            Object attr = httpRequest.getAttribute("currentMerchantNo");
            if (attr != null) merchantNo = attr.toString();
        }
        return merchantNo;
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
