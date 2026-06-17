package com.payhub.settlement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.Result;
import com.payhub.common.result.ResultCode;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.mapper.PayOrderMapper;
import com.payhub.settlement.entity.PaySplitDetail;
import com.payhub.settlement.entity.PaySplitRule;
import com.payhub.settlement.service.SplitEngineService;
import com.payhub.settlement.service.SplitRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/split-detail")
public class SplitDetailController {

    @Autowired
    private SplitEngineService splitEngineService;

    @Autowired
    private PayOrderMapper payOrderMapper;

    @Autowired
    private SplitRuleService splitRuleService;

    @GetMapping("/list")
    public Result<IPage<PaySplitDetail>> list(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String merchantNo,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long settlementId,
            @RequestParam(required = false) Integer status) {
        Map<String, Object> params = new HashMap<>();
        params.put("merchantNo", merchantNo);
        params.put("orderNo", orderNo);
        params.put("settlementId", settlementId);
        params.put("status", status);
        IPage<PaySplitDetail> page = splitEngineService.listSplitDetails(current, size, params);
        return Result.success(page);
    }

    @GetMapping("/order/{orderNo}")
    public Result<List<PaySplitDetail>> getByOrderNo(@PathVariable String orderNo) {
        List<PaySplitDetail> details = splitEngineService.getSplitDetailsByOrderNo(orderNo);
        return Result.success(details);
    }

    @PostMapping("/calculate")
    public Result<List<PaySplitDetail>> calculate(@RequestParam String orderNo) {
        PayOrder order = payOrderMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PayOrder>()
                        .eq(PayOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }

        List<PaySplitRule> rules = splitRuleService.listByMerchantNo(order.getMerchantNo());
        if (rules == null || rules.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "商户无分账规则");
        }

        PaySplitRule enabledRule = null;
        for (PaySplitRule rule : rules) {
            if (rule.getStatus() != null && rule.getStatus() == 1) {
                enabledRule = rule;
                break;
            }
        }

        if (enabledRule == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "商户无启用的分账规则");
        }

        List<PaySplitDetail> details = splitEngineService.calculateSplit(order, enabledRule);
        return Result.success(details);
    }
}
