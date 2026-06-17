package com.payhub.risk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.risk.dto.RiskCheckRequest;
import com.payhub.risk.dto.RiskCheckResult;
import com.payhub.risk.dto.RiskLogVO;
import com.payhub.risk.entity.RiskControlLog;
import com.payhub.risk.mapper.RiskControlLogMapper;
import com.payhub.risk.service.RiskControlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RiskControlServiceImpl extends ServiceImpl<RiskControlLogMapper, RiskControlLog> implements RiskControlService {

    private static final String IP_BLACKLIST_KEY = "risk:ip_blacklist:";
    private static final String FREQUENCY_KEY_PREFIX = "risk:frequency:";
    private static final int SINGLE_AMOUNT_LIMIT = 5000000;
    private static final int DAILY_AMOUNT_LIMIT = 50000000;
    private static final int IP_FREQUENCY_LIMIT = 100;
    private static final int IP_FREQUENCY_WINDOW_SECONDS = 60;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public RiskCheckResult checkRisk(RiskCheckRequest request) {
        List<String> riskRules = new ArrayList<>();
        int maxRiskLevel = 0;
        StringBuilder riskDesc = new StringBuilder();

        if (checkIpBlacklist(request.getClientIp())) {
            riskRules.add("IP_BLACKLIST");
            maxRiskLevel = Math.max(maxRiskLevel, 3);
            riskDesc.append("IP在黑名单中; ");
        }

        if (checkAmountLimit(request.getPayAmount())) {
            riskRules.add("SINGLE_AMOUNT_LIMIT");
            maxRiskLevel = Math.max(maxRiskLevel, 2);
            riskDesc.append("单笔金额超限; ");
        }

        if (checkDailyAmountLimit(request.getMerchantNo(), request.getPayAmount())) {
            riskRules.add("DAILY_AMOUNT_LIMIT");
            maxRiskLevel = Math.max(maxRiskLevel, 2);
            riskDesc.append("日累计金额超限; ");
        }

        if (checkFrequencyLimit(request.getClientIp(), request.getMerchantNo())) {
            riskRules.add("FREQUENCY_LIMIT");
            maxRiskLevel = Math.max(maxRiskLevel, 2);
            riskDesc.append("请求频率超限; ");
        }

        if (checkAbnormalBehavior(request)) {
            riskRules.add("ABNORMAL_BEHAVIOR");
            maxRiskLevel = Math.max(maxRiskLevel, 3);
            riskDesc.append("检测到异常行为; ");
        }

        boolean pass = riskRules.isEmpty();
        saveRiskLog(request, riskRules, maxRiskLevel, riskDesc.toString(), pass);

        return RiskCheckResult.builder()
                .pass(pass)
                .riskLevel(maxRiskLevel)
                .riskRules(riskRules)
                .riskDesc(riskDesc.toString())
                .suggestion(pass ? "风控校验通过" : "建议人工审核或拒绝交易")
                .build();
    }

    @Override
    public IPage<RiskLogVO> listRiskLogs(Long current, Long size, Map<String, Object> params) {
        LambdaQueryWrapper<RiskControlLog> wrapper = new LambdaQueryWrapper<>();
        if (params.get("merchantNo") != null) {
            wrapper.eq(RiskControlLog::getMerchantNo, params.get("merchantNo"));
        }
        if (params.get("riskType") != null) {
            wrapper.eq(RiskControlLog::getRiskType, params.get("riskType"));
        }
        if (params.get("riskLevel") != null) {
            wrapper.eq(RiskControlLog::getRiskLevel, params.get("riskLevel"));
        }
        if (params.get("clientIp") != null) {
            wrapper.eq(RiskControlLog::getClientIp, params.get("clientIp"));
        }
        wrapper.orderByDesc(RiskControlLog::getTriggerTime);

        IPage<RiskControlLog> page = this.page(new Page<>(current, size), wrapper);
        return page.convert(this::convertToVO);
    }

    @Override
    public void addIpToBlacklist(String ip, String reason) {
        redisTemplate.opsForValue().set(IP_BLACKLIST_KEY + ip, reason, 7, TimeUnit.DAYS);
        log.info("IP[{}]已加入黑名单，原因：{}", ip, reason);
    }

    @Override
    public void removeIpFromBlacklist(String ip) {
        redisTemplate.delete(IP_BLACKLIST_KEY + ip);
        log.info("IP[{}]已从黑名单移除", ip);
    }

    @Override
    public boolean isIpInBlacklist(String ip) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(IP_BLACKLIST_KEY + ip));
    }

    private boolean checkIpBlacklist(String ip) {
        return isIpInBlacklist(ip);
    }

    private boolean checkAmountLimit(BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        return amount.compareTo(new BigDecimal(SINGLE_AMOUNT_LIMIT)) > 0;
    }

    private boolean checkDailyAmountLimit(String merchantNo, BigDecimal amount) {
        if (amount == null || StrUtil.isBlank(merchantNo)) {
            return false;
        }
        String key = "risk:daily_amount:" + merchantNo + ":" + LocalDateTime.now().toLocalDate();
        Object totalObj = redisTemplate.opsForValue().get(key);
        BigDecimal total = totalObj != null ? new BigDecimal(totalObj.toString()) : BigDecimal.ZERO;
        return total.add(amount).compareTo(new BigDecimal(DAILY_AMOUNT_LIMIT)) > 0;
    }

    private boolean checkFrequencyLimit(String clientIp, String merchantNo) {
        String key = FREQUENCY_KEY_PREFIX + clientIp + ":" + merchantNo;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, IP_FREQUENCY_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        return count != null && count > IP_FREQUENCY_LIMIT;
    }

    private boolean checkAbnormalBehavior(RiskCheckRequest request) {
        if (StrUtil.isBlank(request.getUserIdentity()) && request.getPayAmount() != null
                && request.getPayAmount().compareTo(new BigDecimal("100000")) > 0) {
            return true;
        }
        return false;
    }

    private void saveRiskLog(RiskCheckRequest request, List<String> riskRules, int riskLevel, String riskDesc, boolean pass) {
        if (riskRules.isEmpty()) {
            return;
        }
        RiskControlLog logEntity = new RiskControlLog();
        logEntity.setMerchantNo(request.getMerchantNo());
        logEntity.setOrderNo(request.getOrderNo());
        logEntity.setRiskType(riskRules.get(0));
        logEntity.setRiskLevel(riskLevel);
        logEntity.setRiskRule(String.join(",", riskRules));
        logEntity.setRiskDesc(riskDesc);
        logEntity.setClientIp(request.getClientIp());
        logEntity.setUserIdentity(request.getUserIdentity());
        logEntity.setPayAmount(request.getPayAmount());
        logEntity.setPayChannel(request.getPayChannel());
        logEntity.setRequestParams(JSON.toJSONString(request));
        logEntity.setHandleResult(pass ? 1 : 0);
        logEntity.setHandleDesc(pass ? "通过" : "拦截");
        logEntity.setTriggerTime(LocalDateTime.now());
        this.save(logEntity);
    }

    private RiskLogVO convertToVO(RiskControlLog entity) {
        return RiskLogVO.builder()
                .id(entity.getId())
                .merchantNo(entity.getMerchantNo())
                .orderNo(entity.getOrderNo())
                .riskType(entity.getRiskType())
                .riskLevel(entity.getRiskLevel())
                .riskRule(entity.getRiskRule())
                .riskDesc(entity.getRiskDesc())
                .clientIp(entity.getClientIp())
                .userIdentity(entity.getUserIdentity())
                .payAmount(entity.getPayAmount())
                .payChannel(entity.getPayChannel())
                .handleResult(entity.getHandleResult())
                .handleDesc(entity.getHandleDesc())
                .triggerTime(entity.getTriggerTime())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
