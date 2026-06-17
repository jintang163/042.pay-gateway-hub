package com.payhub.risk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.risk.dto.*;
import com.payhub.risk.engine.DroolsRuleEngine;
import com.payhub.risk.entity.RiskAuditRecord;
import com.payhub.risk.entity.RiskControlLog;
import com.payhub.risk.entity.RiskDeviceFingerprint;
import com.payhub.risk.entity.RiskWhitelist;
import com.payhub.risk.enums.ActionTypeEnum;
import com.payhub.risk.enums.AuditStatusEnum;
import com.payhub.risk.enums.ListTypeEnum;
import com.payhub.risk.enums.RiskLevelEnum;
import com.payhub.risk.mapper.RiskControlLogMapper;
import com.payhub.risk.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RiskControlServiceImpl extends ServiceImpl<RiskControlLogMapper, RiskControlLog> implements RiskControlService {

    private static final String IP_BLACKLIST_KEY = "risk:ip_blacklist:";
    private static final String FREQUENCY_KEY_PREFIX = "risk:freq:";
    private static final String DAILY_AMOUNT_KEY_PREFIX = "risk:daily_amount:";
    private static final int FREQUENCY_WINDOW_SECONDS = 60;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DroolsRuleEngine droolsRuleEngine;

    @Autowired
    private RiskBlacklistService riskBlacklistService;

    @Autowired
    private RiskWhitelistService riskWhitelistService;

    @Autowired
    private RiskDeviceService riskDeviceService;

    @Autowired
    private RiskAuditService riskAuditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiskCheckResult checkRisk(RiskCheckRequest request) {
        log.info("开始风控检查，merchantNo：{}，orderNo：{}，ip：{}",
                request.getMerchantNo(), request.getOrderNo(), request.getClientIp());

        RiskFact fact = RiskFact.builder()
                .merchantNo(request.getMerchantNo())
                .orderNo(request.getOrderNo())
                .clientIp(request.getClientIp())
                .userIdentity(request.getUserIdentity())
                .deviceId(request.getRequestParams() != null ? extractDeviceId(request.getRequestParams()) : null)
                .payAmount(request.getPayAmount())
                .payChannel(request.getPayChannel())
                .payType(request.getPayType())
                .requestTime(LocalDateTime.now())
                .build();

        boolean whitelistedPassAll = checkWhitelist(fact);
        if (whitelistedPassAll) {
            log.info("白名单命中且免检全部规则，直接通过，merchantNo：{}", request.getMerchantNo());
            RiskControlLog logEntity = saveRiskLog(request, fact, true, ActionTypeEnum.PASS.getCode(), null);
            return buildResult(fact, true, null, logEntity);
        }

        checkBlacklist(fact);

        handleDeviceFingerprint(fact, request);

        checkFrequency(fact);

        checkDailyAmount(fact);

        fact = droolsRuleEngine.executeRules(fact);
        log.debug("Drools规则执行完成，riskLevel：{}，actionType：{}，matchedRules：{}",
                fact.getRiskLevel(), fact.getActionType(), fact.getMatchedRules());

        boolean pass = true;
        String actionType = ActionTypeEnum.PASS.getCode();
        RiskAuditRecord auditRecord = null;
        boolean auditRequired = false;
        boolean smsRequired = false;

        int level = fact.getRiskLevel() != null ? fact.getRiskLevel() : 0;
        String factActionType = fact.getActionType();

        if (level >= RiskLevelEnum.HIGH.getCode()) {
            if (ActionTypeEnum.BLOCK.getCode().equalsIgnoreCase(factActionType)
                    || Boolean.TRUE.equals(fact.getBlocked())
                    || Boolean.TRUE.equals(fact.getBlacklisted())) {
                pass = false;
                actionType = ActionTypeEnum.BLOCK.getCode();
                log.info("高风险拦截，merchantNo：{}，orderNo：{}，level：{}",
                        request.getMerchantNo(), request.getOrderNo(), level);
            } else if (ActionTypeEnum.MANUAL.getCode().equalsIgnoreCase(factActionType)) {
                pass = false;
                actionType = ActionTypeEnum.MANUAL.getCode();
                auditRequired = true;
            }
        } else if (level == RiskLevelEnum.MEDIUM.getCode()) {
            if (ActionTypeEnum.SMS.getCode().equalsIgnoreCase(factActionType)) {
                pass = false;
                actionType = ActionTypeEnum.SMS.getCode();
                smsRequired = true;
                auditRequired = true;
            } else if (ActionTypeEnum.MANUAL.getCode().equalsIgnoreCase(factActionType)) {
                pass = false;
                actionType = ActionTypeEnum.MANUAL.getCode();
                auditRequired = true;
            }
        } else if (level == RiskLevelEnum.LOW.getCode()) {
            pass = true;
            actionType = ActionTypeEnum.PASS.getCode();
            log.info("低风险放行，merchantNo：{}，orderNo：{}，level：{}",
                    request.getMerchantNo(), request.getOrderNo(), level);
        }

        RiskControlLog logEntity = saveRiskLog(request, fact, pass, actionType, null);

        if (auditRequired) {
            auditRecord = riskAuditService.createAudit(
                    logEntity.getId(),
                    ActionTypeEnum.MANUAL.getCode().equalsIgnoreCase(actionType) ? "MANUAL" : "SMS",
                    level);
            logEntity.setAuditId(auditRecord.getId());
            logEntity.setAuditStatus(AuditStatusEnum.PENDING.getCode());
            this.updateById(logEntity);
        }

        return buildResult(fact, pass, auditRecord, logEntity);
    }

    private boolean checkWhitelist(RiskFact fact) {
        boolean ipInWhitelist = riskWhitelistService.checkInList(ListTypeEnum.IP.getCode(), fact.getClientIp());
        boolean userInWhitelist = StrUtil.isNotBlank(fact.getUserIdentity())
                && riskWhitelistService.checkInList(ListTypeEnum.USER.getCode(), fact.getUserIdentity());
        boolean merchantInWhitelist = StrUtil.isNotBlank(fact.getMerchantNo())
                && riskWhitelistService.checkInList(ListTypeEnum.MERCHANT.getCode(), fact.getMerchantNo());
        boolean deviceInWhitelist = StrUtil.isNotBlank(fact.getDeviceId())
                && riskWhitelistService.checkInList(ListTypeEnum.DEVICE.getCode(), fact.getDeviceId());

        boolean whitelisted = ipInWhitelist || userInWhitelist || merchantInWhitelist || deviceInWhitelist;
        fact.setWhitelisted(whitelisted);

        if (whitelisted) {
            RiskWhitelist wl = null;
            if (ipInWhitelist) {
                wl = riskWhitelistService.getByTypeAndValue(ListTypeEnum.IP.getCode(), fact.getClientIp());
            } else if (userInWhitelist) {
                wl = riskWhitelistService.getByTypeAndValue(ListTypeEnum.USER.getCode(), fact.getUserIdentity());
            } else if (merchantInWhitelist) {
                wl = riskWhitelistService.getByTypeAndValue(ListTypeEnum.MERCHANT.getCode(), fact.getMerchantNo());
            } else if (deviceInWhitelist) {
                wl = riskWhitelistService.getByTypeAndValue(ListTypeEnum.DEVICE.getCode(), fact.getDeviceId());
            }

            if (wl != null && StrUtil.isBlank(wl.getBypassRules())) {
                return true;
            }
        }
        return false;
    }

    private void checkBlacklist(RiskFact fact) {
        boolean ipInBlacklist = riskBlacklistService.checkInList(ListTypeEnum.IP.getCode(), fact.getClientIp());
        boolean userInBlacklist = StrUtil.isNotBlank(fact.getUserIdentity())
                && riskBlacklistService.checkInList(ListTypeEnum.USER.getCode(), fact.getUserIdentity());
        boolean merchantInBlacklist = StrUtil.isNotBlank(fact.getMerchantNo())
                && riskBlacklistService.checkInList(ListTypeEnum.MERCHANT.getCode(), fact.getMerchantNo());
        boolean deviceInBlacklist = StrUtil.isNotBlank(fact.getDeviceId())
                && riskBlacklistService.checkInList(ListTypeEnum.DEVICE.getCode(), fact.getDeviceId());

        boolean blacklisted = ipInBlacklist || userInBlacklist || merchantInBlacklist || deviceInBlacklist;
        fact.setBlacklisted(blacklisted);
        fact.setIpBlacklisted(ipInBlacklist);

        if (blacklisted) {
            fact.addHitRule("BLACKLIST_HIT", "黑名单命中", RiskLevelEnum.HIGH.getCode());
            fact.setBlocked(true);
            fact.setActionType(ActionTypeEnum.BLOCK.getCode());
        }
    }

    private void handleDeviceFingerprint(RiskFact fact, RiskCheckRequest request) {
        String deviceId = fact.getDeviceId();
        if (deviceId == null) {
            return;
        }
        String userAgent = request.getRequestParams() != null ? extractUserAgent(request.getRequestParams()) : null;

        Map<String, String> deviceInfo = new HashMap<>();
        deviceInfo.put("userAgent", userAgent);
        deviceInfo.put("ip", fact.getClientIp());

        RiskDeviceFingerprint device = riskDeviceService.getOrCreateDevice(deviceId, deviceInfo);

        if (device != null) {
            fact.setDeviceId(device.getDeviceId());
            riskDeviceService.recordDeviceRequest(device.getDeviceId(), fact.getClientIp(),
                    fact.getUserIdentity(), fact.getMerchantNo());
            int riskScore = riskDeviceService.getDeviceRiskScore(device.getDeviceId());
            fact.setDeviceRiskScore(riskScore);
        }
    }

    private void checkFrequency(RiskFact fact) {
        if (StrUtil.isBlank(fact.getClientIp()) || StrUtil.isBlank(fact.getMerchantNo())) {
            fact.setFrequencyCount(0);
            return;
        }
        String key = FREQUENCY_KEY_PREFIX + fact.getClientIp() + ":" + fact.getMerchantNo();
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, FREQUENCY_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        fact.setFrequencyCount(count != null ? count.intValue() : 0);
    }

    private void checkDailyAmount(RiskFact fact) {
        if (StrUtil.isBlank(fact.getMerchantNo()) || fact.getPayAmount() == null) {
            fact.setDailyAmount(BigDecimal.ZERO);
            return;
        }
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = DAILY_AMOUNT_KEY_PREFIX + fact.getMerchantNo() + ":" + dateStr;
        Object totalObj = redisTemplate.opsForValue().get(key);
        BigDecimal total = totalObj != null ? new BigDecimal(totalObj.toString()) : BigDecimal.ZERO;
        BigDecimal newTotal = total.add(fact.getPayAmount());
        redisTemplate.opsForValue().set(key, newTotal, 2, TimeUnit.DAYS);
        fact.setDailyAmount(newTotal);
    }

    @Transactional(rollbackFor = Exception.class)
    public RiskControlLog saveRiskLog(RiskCheckRequest request, RiskFact fact, boolean pass,
                                       String actionType, RiskAuditRecord auditRecord) {
        RiskControlLog logEntity = new RiskControlLog();
        logEntity.setMerchantNo(request.getMerchantNo());
        logEntity.setOrderNo(request.getOrderNo());
        logEntity.setRiskType(fact.getMatchedRules() != null && !fact.getMatchedRules().isEmpty()
                ? fact.getMatchedRules().get(0) : "NORMAL");
        logEntity.setRiskLevel(fact.getRiskLevel() != null ? fact.getRiskLevel() : 0);
        logEntity.setRiskRule(fact.getMatchedRules() != null ? String.join(",", fact.getMatchedRules()) : "");
        logEntity.setRiskDesc(fact.getRiskDesc() != null ? fact.getRiskDesc().toString() : "");
        logEntity.setClientIp(request.getClientIp());
        logEntity.setUserIdentity(request.getUserIdentity());
        logEntity.setPayAmount(request.getPayAmount());
        logEntity.setPayChannel(request.getPayChannel());
        logEntity.setRequestParams(JSON.toJSONString(request));
        logEntity.setHandleResult(pass ? 1 : 0);
        logEntity.setHandleDesc(buildHandleDesc(pass, actionType, fact.getRiskLevel()));
        logEntity.setTriggerTime(LocalDateTime.now());

        if (auditRecord != null) {
            logEntity.setAuditId(auditRecord.getId());
            logEntity.setAuditStatus(auditRecord.getAuditStatus());
        }

        this.save(logEntity);
        log.info("保存风控日志成功，logId：{}，pass：{}，riskLevel：{}",
                logEntity.getId(), pass, logEntity.getRiskLevel());
        return logEntity;
    }

    private String buildHandleDesc(boolean pass, String actionType, Integer riskLevel) {
        if (pass) {
            if (RiskLevelEnum.LOW.getCode().equals(riskLevel)) {
                return "低风险放行";
            }
            return "通过";
        }
        if (ActionTypeEnum.BLOCK.getCode().equalsIgnoreCase(actionType)) {
            return "拦截";
        }
        if (ActionTypeEnum.MANUAL.getCode().equalsIgnoreCase(actionType)) {
            return "待人工审核";
        }
        if (ActionTypeEnum.SMS.getCode().equalsIgnoreCase(actionType)) {
            return "待短信验证";
        }
        return "未通过";
    }

    private RiskCheckResult buildResult(RiskFact fact, boolean pass, RiskAuditRecord auditRecord, RiskControlLog logEntity) {
        RiskCheckResult.RiskCheckResultBuilder builder = RiskCheckResult.builder()
                .pass(pass)
                .riskLevel(fact.getRiskLevel() != null ? fact.getRiskLevel() : 0)
                .riskRules(fact.getMatchedRules() != null ? fact.getMatchedRules() : new ArrayList<>())
                .riskDesc(fact.getRiskDesc() != null ? fact.getRiskDesc().toString() : "")
                .suggestion(buildSuggestion(pass, fact.getRiskLevel(), fact.getActionType()))
                .auditRequired(auditRecord != null)
                .smsRequired(ActionTypeEnum.SMS.getCode().equalsIgnoreCase(fact.getActionType()))
                .whitelisted(Boolean.TRUE.equals(fact.getWhitelisted()));

        if (auditRecord != null) {
            builder.auditId(auditRecord.getId());
        }

        return builder.build();
    }

    private String buildSuggestion(boolean pass, Integer riskLevel, String actionType) {
        if (pass) {
            if (Boolean.TRUE.equals(riskLevel != null && riskLevel == RiskLevelEnum.LOW.getCode())) {
                return "低风险放行，建议持续监控";
            }
            return "风控校验通过";
        }
        if (ActionTypeEnum.BLOCK.getCode().equalsIgnoreCase(actionType)) {
            return "高风险交易，建议拦截或拒绝";
        }
        if (ActionTypeEnum.MANUAL.getCode().equalsIgnoreCase(actionType)) {
            return "建议人工审核后决定";
        }
        if (ActionTypeEnum.SMS.getCode().equalsIgnoreCase(actionType)) {
            return "需要短信二次验证";
        }
        return "建议进一步核查";
    }

    private String extractDeviceId(String requestParams) {
        try {
            if (StrUtil.isBlank(requestParams)) return null;
            com.alibaba.fastjson2.JSONObject obj = JSON.parseObject(requestParams);
            return obj.getString("deviceId");
        } catch (Exception e) {
            return null;
        }
    }

    private String extractUserAgent(String requestParams) {
        try {
            if (StrUtil.isBlank(requestParams)) return null;
            com.alibaba.fastjson2.JSONObject obj = JSON.parseObject(requestParams);
            return obj.getString("userAgent");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public IPage<RiskLogVO> listRiskLogs(Long current, Long size, Map<String, Object> params) {
        LambdaQueryWrapper<RiskControlLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskControlLog::getDeleted, 0);

        if (params != null) {
            if (params.get("merchantNo") != null && StrUtil.isNotBlank(params.get("merchantNo").toString())) {
                wrapper.eq(RiskControlLog::getMerchantNo, params.get("merchantNo").toString());
            }
            if (params.get("riskType") != null && StrUtil.isNotBlank(params.get("riskType").toString())) {
                wrapper.eq(RiskControlLog::getRiskType, params.get("riskType").toString());
            }
            if (params.get("riskLevel") != null) {
                wrapper.eq(RiskControlLog::getRiskLevel, Integer.parseInt(params.get("riskLevel").toString()));
            }
            if (params.get("clientIp") != null && StrUtil.isNotBlank(params.get("clientIp").toString())) {
                wrapper.eq(RiskControlLog::getClientIp, params.get("clientIp").toString());
            }
            if (params.get("auditStatus") != null) {
                wrapper.eq(RiskControlLog::getAuditStatus, Integer.parseInt(params.get("auditStatus").toString()));
            }
        }
        wrapper.orderByDesc(RiskControlLog::getTriggerTime);

        IPage<RiskControlLog> page = this.page(new Page<>(current, size), wrapper);
        return page.convert(this::convertToVO);
    }

    @Override
    public RiskLogVO getRiskLogById(Long id) {
        RiskControlLog entity = this.getById(id);
        if (entity == null || entity.getDeleted() == 1) {
            return null;
        }
        return convertToVO(entity);
    }

    @Override
    public void addIpToBlacklist(String ip, String reason) {
        redisTemplate.opsForValue().set(IP_BLACKLIST_KEY + ip, reason, 7, TimeUnit.DAYS);
        log.info("IP[{}]已加入Redis黑名单，原因：{}", ip, reason);
    }

    @Override
    public void removeIpFromBlacklist(String ip) {
        redisTemplate.delete(IP_BLACKLIST_KEY + ip);
        log.info("IP[{}]已从Redis黑名单移除", ip);
    }

    @Override
    public boolean isIpInBlacklist(String ip) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(IP_BLACKLIST_KEY + ip));
    }

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), java.time.LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), java.time.LocalTime.MAX);

        LambdaQueryWrapper<RiskControlLog> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.between(RiskControlLog::getTriggerTime, todayStart, todayEnd)
                .eq(RiskControlLog::getDeleted, 0);
        List<RiskControlLog> todayLogs = this.list(todayWrapper);

        long todayTotal = todayLogs.size();
        long todayBlocked = todayLogs.stream().filter(l -> l.getHandleResult() != null && l.getHandleResult() == 0).count();
        long todayPassed = todayTotal - todayBlocked;
        long todayHighRisk = todayLogs.stream().filter(l -> RiskLevelEnum.HIGH.getCode().equals(l.getRiskLevel())).count();
        long todayMediumRisk = todayLogs.stream().filter(l -> RiskLevelEnum.MEDIUM.getCode().equals(l.getRiskLevel())).count();
        long todayLowRisk = todayLogs.stream().filter(l -> RiskLevelEnum.LOW.getCode().equals(l.getRiskLevel())).count();

        stats.put("todayTotal", todayTotal);
        stats.put("todayPassed", todayPassed);
        stats.put("todayBlocked", todayBlocked);
        stats.put("todayHighRisk", todayHighRisk);
        stats.put("todayMediumRisk", todayMediumRisk);
        stats.put("todayLowRisk", todayLowRisk);
        stats.put("todayBlockRate", todayTotal > 0
                ? String.format("%.2f", (todayBlocked * 100.0 / todayTotal)) + "%"
                : "0.00%");

        LambdaQueryWrapper<RiskControlLog> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(RiskControlLog::getAuditStatus, AuditStatusEnum.PENDING.getCode())
                .eq(RiskControlLog::getDeleted, 0);
        long pendingAuditCount = this.count(pendingWrapper);
        stats.put("pendingAuditCount", pendingAuditCount);

        LocalDateTime weekStart = LocalDateTime.of(LocalDate.now().minusDays(6), java.time.LocalTime.MIN);
        LambdaQueryWrapper<RiskControlLog> weekWrapper = new LambdaQueryWrapper<>();
        weekWrapper.between(RiskControlLog::getTriggerTime, weekStart, todayEnd)
                .eq(RiskControlLog::getDeleted, 0)
                .orderByAsc(RiskControlLog::getTriggerTime);
        List<RiskControlLog> weekLogs = this.list(weekWrapper);

        Map<String, Map<String, Long>> trendData = new LinkedHashMap<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            String dateKey = LocalDate.now().minusDays(i).format(dayFormatter);
            Map<String, Long> dayStats = new HashMap<>();
            dayStats.put("total", 0L);
            dayStats.put("blocked", 0L);
            trendData.put(dateKey, dayStats);
        }

        for (RiskControlLog log : weekLogs) {
            if (log.getTriggerTime() != null) {
                String dateKey = log.getTriggerTime().format(dayFormatter);
                Map<String, Long> dayStats = trendData.get(dateKey);
                if (dayStats != null) {
                    dayStats.put("total", dayStats.get("total") + 1);
                    if (log.getHandleResult() != null && log.getHandleResult() == 0) {
                        dayStats.put("blocked", dayStats.get("blocked") + 1);
                    }
                }
            }
        }
        stats.put("weekTrend", trendData);

        Map<String, Long> riskTypeCount = new HashMap<>();
        for (RiskControlLog log : todayLogs) {
            String type = StrUtil.isNotBlank(log.getRiskType()) ? log.getRiskType() : "NORMAL";
            riskTypeCount.merge(type, 1L, Long::sum);
        }
        stats.put("riskTypeDistribution", riskTypeCount);

        log.info("获取风控仪表盘统计完成，今日请求：{}，拦截：{}，待审核：{}", todayTotal, todayBlocked, pendingAuditCount);
        return stats;
    }

    private RiskLogVO convertToVO(RiskControlLog entity) {
        AuditStatusEnum statusEnum = AuditStatusEnum.getByCode(entity.getAuditStatus());
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
                .auditStatus(entity.getAuditStatus())
                .auditStatusDesc(statusEnum != null ? statusEnum.getDesc() : "")
                .auditId(entity.getAuditId())
                .triggerTime(entity.getTriggerTime())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
