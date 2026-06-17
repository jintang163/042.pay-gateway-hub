package com.payhub.pay.controller;

import com.payhub.common.result.Result;
import com.payhub.pay.dto.PayConfigSaveRequest;
import com.payhub.pay.dto.PayConfigVO;
import com.payhub.pay.service.MerchantPayConfigService;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pay/config")
public class PayConfigController {

    @Autowired
    private MerchantPayConfigService merchantPayConfigService;

    @GetMapping("/list")
    public Result<List<PayConfigVO>> list(HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        List<PayConfigVO> list = merchantPayConfigService.listByMerchantNo(merchantNo);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<PayConfigVO> getById(@PathVariable Long id) {
        PayConfigVO config = merchantPayConfigService.getConfigById(id);
        return Result.success(config);
    }

    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody PayConfigSaveRequest request,
                           HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        if (merchantNo != null) {
            request.setMerchantNo(merchantNo);
        }
        merchantPayConfigService.saveConfig(request);
        return Result.success();
    }

    @PostMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable Long id) {
        merchantPayConfigService.toggleConfig(id);
        return Result.success();
    }
}
