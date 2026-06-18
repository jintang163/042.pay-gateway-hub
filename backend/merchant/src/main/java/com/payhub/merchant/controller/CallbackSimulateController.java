package com.payhub.merchant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.merchant.dto.*;
import com.payhub.merchant.service.CallbackSimulateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/callback-simulate")
public class CallbackSimulateController {

    @Autowired
    private CallbackSimulateService callbackSimulateService;

    @PostMapping("/send")
    public Result<CallbackSimulateVO> send(@Valid @RequestBody CallbackSimulateRequest request) {
        CallbackSimulateVO vo = callbackSimulateService.simulate(request);
        return Result.success(vo);
    }

    @GetMapping("/list")
    public Result<IPage<CallbackSimulateVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) String callbackType,
            @RequestParam(required = false) String simulateStatus,
            @RequestParam(required = false) Integer callbackStatus) {
        Map<String, Object> params = new HashMap<>();
        params.put("merchantNo", merchantNo);
        params.put("callbackType", callbackType);
        params.put("simulateStatus", simulateStatus);
        params.put("callbackStatus", callbackStatus);
        IPage<CallbackSimulateVO> page = callbackSimulateService.listPage(current, size, params);
        return Result.success(page);
    }

    @PostMapping("/resend")
    public Result<CallbackSimulateVO> resend(@Valid @RequestBody CallbackResendRequest request) {
        CallbackSimulateVO vo = callbackSimulateService.resend(request.getLogNo());
        return Result.success(vo);
    }

    @PostMapping("/sign-code")
    public Result<SignCodeExampleVO> signCode(@Valid @RequestBody SignCodeExampleRequest request) {
        SignCodeExampleVO vo = callbackSimulateService.generateSignCode(request);
        return Result.success(vo);
    }

    @GetMapping("/{logNo}")
    public Result<CallbackSimulateVO> getByLogNo(@PathVariable String logNo) {
        CallbackSimulateVO vo = callbackSimulateService.getByLogNo(logNo);
        return Result.success(vo);
    }
}
