package com.payhub.marketing.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.marketing.dto.ActivitySaveRequest;
import com.payhub.marketing.dto.ActivityVO;
import com.payhub.marketing.service.ActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @GetMapping("/list")
    public Result<IPage<ActivityVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String activityCode,
            @RequestParam(required = false) String activityName,
            @RequestParam(required = false) Integer activityType,
            @RequestParam(required = false) Integer status,
            HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        Map<String, Object> params = new HashMap<>();
        params.put("activityCode", activityCode);
        params.put("activityName", activityName);
        params.put("activityType", activityType);
        params.put("status", status);
        IPage<ActivityVO> page = activityService.listPage(current, size, merchantNo, params);
        return Result.success(page);
    }

    @GetMapping("/{activityCode}")
    public Result<ActivityVO> getByActivityCode(@PathVariable String activityCode) {
        return Result.success(activityService.getByActivityCode(activityCode));
    }

    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody ActivitySaveRequest request,
                             HttpServletRequest httpRequest) {
        String merchantNo = (String) httpRequest.getAttribute("currentMerchantNo");
        if (merchantNo != null) {
            request.setMerchantNo(merchantNo);
        }
        activityService.saveActivity(request);
        return Result.success();
    }

    @PostMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable Long id) {
        activityService.toggleStatus(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        activityService.deleteActivity(id);
        return Result.success();
    }
}
