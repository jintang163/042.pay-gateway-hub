package com.payhub.pay.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payhub.channel.entity.PayChannelLog;
import com.payhub.channel.mapper.PayChannelLogMapper;
import com.payhub.common.enums.FailReasonEnum;
import com.payhub.common.enums.PayStatusEnum;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.risk.enums.RiskLevelEnum;
import com.payhub.pay.dto.vo.OrderAttributionVO;
import com.payhub.pay.dto.vo.PayOrderBriefVO;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.mapper.PayOrderMapper;
import com.payhub.pay.service.OrderAttributionService;
import com.payhub.risk.entity.RiskControlLog;
import com.payhub.risk.mapper.RiskControlLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class OrderAttributionServiceImpl implements OrderAttributionService {

    private static final int CHANNEL_TIMEOUT_THRESHOLD_MS = 10000;
    private static final List<String> BALANCE_KEYWORDS = Arrays.asList(
            "NOT_ENOUGH_MONEY", "INSUFFICIENT", "NOTENOUGH", "余额不足",
            "INSUFFICIENT_FUNDS", "NO_BALANCE", "LOW_BALANCE"
    );
    private static final List<String> TIMEOUT_KEYWORDS = Arrays.asList(
            "timeout", "timed out", "connect timed out", "read timed out",
            "socket timeout", "connection reset", "超时", "超时错误"
    );
    private static final List<String> SYSTEM_ERROR_KEYWORDS = Arrays.asList(
            "SYSTEM_ERROR", "SERVER_ERROR", "SYSTEM_EXCEPTION", "SYSTEM BUSY",
            "服务异常", "系统繁忙", "SYSTEM_UNAVAILABLE", "BUSY"
    );
    private static final List<String> SIGN_ERROR_KEYWORDS = Arrays.asList(
            "SIGN_ERROR", "INVALID_SIGN", "SIGN_VERIFY_FAIL", "签名错误",
            "验签失败", "ILLEGAL_SIGN", "sign check fail"
    );
    private static final List<String> PARAM_ERROR_KEYWORDS = Arrays.asList(
            "INVALID_PARAM", "ILLEGAL_ARGUMENT", "PARAM_ERROR", "参数错误",
            "缺少参数", "参数非法", "INVALID_ARGUMENT", "MISSING_PARAM"
    );

    @Autowired
    private PayOrderMapper payOrderMapper;

    @Autowired
    private PayChannelLogMapper payChannelLogMapper;

    @Autowired
    private RiskControlLogMapper riskControlLogMapper;

    @Override
    public OrderAttributionVO analyzeFailReason(String orderNo, String merchantNo) {
        PayOrder order = payOrderMapper.selectOne(
                new LambdaQueryWrapper<PayOrder>()
                        .eq(PayOrder::getOrderNo, orderNo)
                        .eq(merchantNo != null, PayOrder::getMerchantNo, merchantNo)
        );
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "订单不存在");
        }

        OrderAttributionVO vo = new OrderAttributionVO();
        PayOrderBriefVO brief = new PayOrderBriefVO();
        BeanUtils.copyProperties(order, brief);
        vo.setOrderInfo(brief);
        vo.setOrderNo(order.getOrderNo());

        if (!PayStatusEnum.FAIL.getCode().equals(order.getPayStatus())
                && !PayStatusEnum.CLOSED.getCode().equals(order.getPayStatus())) {
            vo.setFailCode(null);
            vo.setFailMessage("订单未失败，无需归因");
            vo.setSuggestion("当前订单状态正常，无需处理");
            vo.setEvidence(Arrays.asList("订单状态: " + order.getPayStatus()));
            return vo;
        }

        PayChannelLog channelLog = payChannelLogMapper.selectOne(
                new LambdaQueryWrapper<PayChannelLog>()
                        .eq(PayChannelLog::getOrderNo, orderNo)
                        .orderByDesc(PayChannelLog::getCreateTime)
                        .last("LIMIT 1")
        );
        vo.setLatestChannelLog(channelLog);

        RiskControlLog riskLog = riskControlLogMapper.selectOne(
                new LambdaQueryWrapper<RiskControlLog>()
                        .eq(RiskControlLog::getOrderNo, orderNo)
                        .orderByDesc(RiskControlLog::getCreatedAt)
                        .last("LIMIT 1")
        );
        vo.setLatestRiskLog(riskLog);

        List<String> evidence = new ArrayList<>();
        FailReasonEnum reason = matchReason(order, channelLog, riskLog, evidence);
        vo.setFailCode(reason.getCode());
        vo.setFailMessage(reason.getMessage());
        vo.setFailCategory(reason.getCategory());
        vo.setSuggestion(reason.getSuggestion());
        vo.setRuleDescription(reason.getRuleDescription());
        vo.setPriority(reason.getPriority());
        vo.setEvidence(evidence);

        log.info("订单[{}]归因分析: code={}, msg={}, evidence={}", orderNo, reason.getCode(), reason.getMessage(), evidence);
        return vo;
    }

    private FailReasonEnum matchReason(PayOrder order, PayChannelLog channelLog,
                                       RiskControlLog riskLog, List<String> evidence) {

        if (PayStatusEnum.CLOSED.getCode().equals(order.getPayStatus())) {
            if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
                evidence.add("订单 expireTime=" + order.getExpireTime() + " 已过期");
                return FailReasonEnum.EXPIRED;
            }
            evidence.add("订单已关闭，推测为用户主动取消");
            return FailReasonEnum.USER_CANCEL;
        }

        if (riskLog != null && isRiskRejected(riskLog)) {
            evidence.add("命中风控规则: " + riskLog.getRiskRule() + " / " + riskLog.getRiskDesc());
            evidence.add("风险等级: " + (riskLog.getRiskLevel() != null
                    ? RiskLevelEnum.getByCode(riskLog.getRiskLevel()).getDesc()
                    : "未知"));
            evidence.add("处理结果: " + riskLog.getHandleResult()
                    + (riskLog.getHandleDesc() != null ? " / " + riskLog.getHandleDesc() : ""));
            return FailReasonEnum.RISK_REJECT;
        }

        if (channelLog == null) {
            evidence.add("pay_channel_log 中未找到该订单记录，可能在业务校验层即失败");
            if (order.getActualAmount() == null) {
                evidence.add("推测为参数或商户限额类问题，actualAmount 为空");
                return FailReasonEnum.PARAM_INVALID;
            }
            return FailReasonEnum.UNKNOWN;
        }

        String responseData = str(channelLog.getResponseData());
        String errorMsg = str(channelLog.getErrorMsg());
        String combined = responseData.toUpperCase() + " " + errorMsg.toUpperCase();

        if (channelLog.getCostTime() != null && channelLog.getCostTime() >= CHANNEL_TIMEOUT_THRESHOLD_MS) {
            evidence.add("通道耗时 costTime=" + channelLog.getCostTime() + "ms 超过阈值 " + CHANNEL_TIMEOUT_THRESHOLD_MS + "ms");
            return FailReasonEnum.CHANNEL_TIMEOUT;
        }

        if (containsAny(combined, TIMEOUT_KEYWORDS) || containsAny(errorMsg, TIMEOUT_KEYWORDS)) {
            evidence.add("channelLog errorMsg/responseData 中含 timeout 关键字: " + takeMatched(combined, TIMEOUT_KEYWORDS));
            return FailReasonEnum.CHANNEL_TIMEOUT;
        }

        if (containsAny(combined, BALANCE_KEYWORDS)) {
            evidence.add("通道响应含余额不足关键字: " + takeMatched(combined, BALANCE_KEYWORDS));
            return FailReasonEnum.INSUFFICIENT_BALANCE;
        }

        if (containsAny(combined, SIGN_ERROR_KEYWORDS) || containsAny(errorMsg, SIGN_ERROR_KEYWORDS)) {
            evidence.add("检测到签名错误关键字: " + takeMatched(combined, SIGN_ERROR_KEYWORDS));
            return FailReasonEnum.SIGN_VERIFY_FAILED;
        }

        if (containsAny(combined, PARAM_ERROR_KEYWORDS) || containsAny(errorMsg, PARAM_ERROR_KEYWORDS)) {
            evidence.add("通道返回参数错误: " + takeMatched(combined, PARAM_ERROR_KEYWORDS));
            return FailReasonEnum.PARAM_INVALID;
        }

        if (containsAny(combined, SYSTEM_ERROR_KEYWORDS) || containsAny(errorMsg, SYSTEM_ERROR_KEYWORDS)) {
            evidence.add("通道返回系统异常: " + takeMatched(combined, SYSTEM_ERROR_KEYWORDS));
            return FailReasonEnum.CHANNEL_SYSTEM_ERROR;
        }

        if (StrUtil.isNotBlank(errorMsg) && responseData.length() < 10) {
            evidence.add("通道 errorMsg 非空: " + errorMsg + "，无法匹配具体原因");
            return FailReasonEnum.CHANNEL_SYSTEM_ERROR;
        }

        if (responseData.isEmpty()) {
            evidence.add("通道响应体为空，疑似通道未返回");
            return FailReasonEnum.CHANNEL_TIMEOUT;
        }

        evidence.add("已采集到通道响应，但关键字未命中已知规则");
        if (responseData.length() > 120) {
            evidence.add("响应片段: " + responseData.substring(0, 120) + "...");
        } else {
            evidence.add("响应片段: " + responseData);
        }
        return FailReasonEnum.UNKNOWN;
    }

    private boolean isRiskRejected(RiskControlLog riskLog) {
        if (riskLog.getHandleResult() != null && riskLog.getHandleResult() == 0) {
            return true;
        }
        if (riskLog.getRiskLevel() != null && riskLog.getRiskLevel() >= RiskLevelEnum.HIGH.getCode()) {
            String handleDesc = str(riskLog.getHandleDesc());
            return handleDesc.contains("拦截") || handleDesc.contains("BLOCK");
        }
        return false;
    }

    private String str(String s) {
        return s == null ? "" : s;
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (text == null || text.isEmpty()) return false;
        for (String kw : keywords) {
            if (text.contains(kw.toUpperCase())) return true;
        }
        return false;
    }

    private String takeMatched(String text, List<String> keywords) {
        if (text == null) return "";
        String t = text.toUpperCase();
        for (String kw : keywords) {
            if (t.contains(kw.toUpperCase())) return kw;
        }
        return "";
    }
}
