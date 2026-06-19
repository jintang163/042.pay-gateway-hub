package com.payhub.marketing.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.marketing.dto.PayLinkSaveRequest;
import com.payhub.marketing.dto.PayLinkVO;
import com.payhub.marketing.service.PayLinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pay-link")
public class PayLinkController {

    @Autowired
    private PayLinkService payLinkService;

    @GetMapping("/list")
    public Result<IPage<PayLinkVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String linkCode,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        Map<String, Object> params = new HashMap<>();
        params.put("linkCode", linkCode);
        params.put("title", title);
        params.put("status", status);
        params.put("startTime", startTime);
        params.put("endTime", endTime);
        IPage<PayLinkVO> page = payLinkService.listPage(current, size, merchantNo, params);
        return Result.success(page);
    }

    @GetMapping("/{linkCode}")
    public Result<PayLinkVO> getByLinkCode(@PathVariable String linkCode) {
        return Result.success(payLinkService.getByLinkCode(linkCode));
    }

    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody PayLinkSaveRequest request,
                             HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        if (merchantNo != null) {
            request.setMerchantNo(merchantNo);
        }
        payLinkService.saveLink(request);
        return Result.success();
    }

    @PostMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable Long id) {
        payLinkService.toggleStatus(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        payLinkService.deleteLink(id);
        return Result.success();
    }

    @GetMapping("/resolve/{linkCode}")
    public Result<PayLinkVO> resolveLink(@PathVariable String linkCode) {
        return Result.success(payLinkService.resolveLink(linkCode));
    }
}
