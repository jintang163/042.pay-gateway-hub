package com.payhub.risk.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.risk.dto.SandboxTestRequest;
import com.payhub.risk.dto.SandboxTestResultVO;
import com.payhub.risk.service.SandboxTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sandbox")
public class SandboxController {

    @Autowired
    private SandboxTestService sandboxTestService;

    @GetMapping("/scenes")
    public Result<List<Map<String, Object>>> listTestScenes() {
        List<Map<String, Object>> scenes = sandboxTestService.listTestScenes();
        return Result.success(scenes);
    }

    @PostMapping("/test")
    public Result<SandboxTestResultVO> executeTest(@Valid @RequestBody SandboxTestRequest request) {
        SandboxTestResultVO result = sandboxTestService.executeTest(request);
        return Result.success(result);
    }

    @GetMapping("/records")
    public Result<IPage<SandboxTestResultVO>> listTestRecords(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) String testScene,
            @RequestParam(required = false) Integer success) {
        Map<String, Object> params = new HashMap<>();
        params.put("merchantNo", merchantNo);
        params.put("testScene", testScene);
        params.put("success", success);
        IPage<SandboxTestResultVO> page = sandboxTestService.listTestRecords(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/records/{testId}")
    public Result<SandboxTestResultVO> getTestRecord(@PathVariable String testId) {
        SandboxTestResultVO record = sandboxTestService.getTestRecord(testId);
        return Result.success(record);
    }
}
