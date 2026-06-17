package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.result.Result;
import com.payhub.settlement.dto.SettlementVO;
import com.payhub.settlement.service.SettlementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/settlement/settlement")
public class SettlementController {

    @Autowired
    private SettlementService settlementService;

    @GetMapping("/{settlementNo}")
    public Result<SettlementVO> getBySettlementNo(@PathVariable String settlementNo) {
        SettlementVO vo = settlementService.getBySettlementNo(settlementNo);
        return Result.success(vo);
    }

    @GetMapping("/list")
    public Result<IPage<SettlementVO>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate settleDate,
            @RequestParam(required = false) Integer settleStatus) {
        Map<String, Object> params = new HashMap<>();
        params.put("merchantNo", merchantNo);
        params.put("settleDate", settleDate);
        params.put("settleStatus", settleStatus);
        IPage<SettlementVO> page = settlementService.listPage(current, size, params);
        return Result.success(page);
    }

    @PostMapping("/generate")
    public Result<Void> generateSettlement(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate settleDate) {
        settlementService.generateSettlement(settleDate);
        return Result.success();
    }

    @PostMapping("/confirm/{id}")
    public Result<Void> confirmSettlement(@PathVariable Long id) {
        settlementService.confirmSettlement(id);
        return Result.success();
    }
}
