package com.payhub.pay.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.pay.dto.RefundRequest;
import com.payhub.pay.dto.RefundResponse;
import com.payhub.pay.entity.PayRefund;
import com.payhub.pay.service.PayRefundService;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pay/refund")
public class PayRefundController {

    @Autowired
    private PayRefundService payRefundService;

    @PostMapping("/apply")
    public Result<RefundResponse> applyRefund(@Valid @RequestBody RefundRequest request,
                                             HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        if (merchantNo != null) {
            request.setMerchantNo(merchantNo);
        }
        String clientIp = getClientIp(httpRequest);
        RefundResponse response = payRefundService.applyRefund(request, clientIp);
        return Result.success(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @PostMapping("/query")
    public Result<RefundResponse> queryRefund(@RequestParam String refundNo) {
        RefundResponse response = payRefundService.queryRefund(refundNo);
        return Result.success(response);
    }

    @GetMapping("/list")
    public Result<IPage<PayRefund>> refundList(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String refundNo,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String merchantRefundNo,
            @RequestParam(required = false) Integer refundStatus,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        Map<String, Object> params = new HashMap<>();
        params.put("refundNo", refundNo);
        params.put("orderNo", orderNo);
        params.put("merchantRefundNo", merchantRefundNo);
        params.put("refundStatus", refundStatus);
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        IPage<PayRefund> page = payRefundService.listPage(current, size, merchantNo, params);
        return Result.success(page);
    }

    @PostMapping("/retry/{refundNo}")
    public Result<Void> retryRefund(@PathVariable String refundNo) {
        payRefundService.retryRefund(refundNo);
        return Result.success();
    }
}
