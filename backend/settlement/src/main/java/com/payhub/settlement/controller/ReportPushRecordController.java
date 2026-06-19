package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.context.CurrentUserContext;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.Result;
import com.payhub.common.result.ResultCode;
import com.payhub.settlement.dto.ReportPushRecordVO;
import com.payhub.settlement.service.ReportPushRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/report/push-record")
public class ReportPushRecordController {

    @Autowired
    private ReportPushRecordService reportPushRecordService;

    @GetMapping("/page")
    public Result<IPage<ReportPushRecordVO>> listPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam Map<String, Object> params) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        if (params == null) {
            params = new HashMap<>();
        }
        params.put("merchantNo", merchantNo);
        IPage<ReportPushRecordVO> page = reportPushRecordService.listPage(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result<ReportPushRecordVO> getById(@PathVariable Long id) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        if (!reportPushRecordService.validateOwnership(id, merchantNo)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权限查看该推送记录");
        }
        ReportPushRecordVO vo = reportPushRecordService.getRecordById(id);
        return Result.success(vo);
    }
}
