package com.payhub.risk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.risk.dto.RiskDeviceVO;
import com.payhub.risk.entity.RiskDeviceFingerprint;
import com.payhub.risk.mapper.RiskDeviceFingerprintMapper;
import com.payhub.risk.service.RiskDeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RiskDeviceServiceImpl extends ServiceImpl<RiskDeviceFingerprintMapper, RiskDeviceFingerprint> implements RiskDeviceService {

    private static final String DEVICE_KEY_PREFIX = "risk:device:";
    private static final long CACHE_EXPIRE_HOURS = 24;
    private static final int MAX_RISK_SCORE = 100;
    private static final int MIN_RISK_SCORE = 0;
    private static final int ABNORMAL_THRESHOLD = 80;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiskDeviceFingerprint getOrCreateDevice(String deviceId, Map<String, String> deviceInfo) {
        if (StrUtil.isBlank(deviceId)) {
            throw new BusinessException("设备ID不能为空");
        }

        String cacheKey = DEVICE_KEY_PREFIX + deviceId;
        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
        if (cachedObj != null && cachedObj instanceof RiskDeviceFingerprint) {
            return (RiskDeviceFingerprint) cachedObj;
        }

        RiskDeviceFingerprint device = this.getOne(new LambdaQueryWrapper<RiskDeviceFingerprint>()
                .eq(RiskDeviceFingerprint::getDeviceId, deviceId));

        if (device == null) {
            device = new RiskDeviceFingerprint();
            device.setDeviceId(deviceId);
            device.setDeviceType(getDeviceInfoValue(deviceInfo, "deviceType"));
            device.setOsType(getDeviceInfoValue(deviceInfo, "osType"));
            device.setOsVersion(getDeviceInfoValue(deviceInfo, "osVersion"));
            device.setBrowserType(getDeviceInfoValue(deviceInfo, "browserType"));
            device.setBrowserVersion(getDeviceInfoValue(deviceInfo, "browserVersion"));
            device.setScreenResolution(getDeviceInfoValue(deviceInfo, "screenResolution"));
            device.setLanguage(getDeviceInfoValue(deviceInfo, "language"));
            device.setTimezone(getDeviceInfoValue(deviceInfo, "timezone"));
            device.setUserAgent(getDeviceInfoValue(deviceInfo, "userAgent"));
            device.setFirstSeenIp(getDeviceInfoValue(deviceInfo, "ip"));
            device.setLastSeenIp(getDeviceInfoValue(deviceInfo, "ip"));
            device.setFirstSeenTime(LocalDateTime.now());
            device.setLastSeenTime(LocalDateTime.now());
            device.setTotalRequestCount(0L);
            device.setRiskScore(0);
            device.setStatus(1);
            this.save(device);
            log.info("创建设备指纹成功，deviceId：{}", deviceId);
        }

        redisTemplate.opsForValue().set(cacheKey, device, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return device;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDeviceRisk(String deviceId, int scoreDelta, String tag) {
        if (StrUtil.isBlank(deviceId)) {
            return;
        }

        RiskDeviceFingerprint device = this.getOrCreateDevice(deviceId, null);
        if (device == null) {
            return;
        }

        int newScore = device.getRiskScore() != null ? device.getRiskScore() + scoreDelta : scoreDelta;
        newScore = Math.max(MIN_RISK_SCORE, Math.min(MAX_RISK_SCORE, newScore));
        device.setRiskScore(newScore);

        if (StrUtil.isNotBlank(tag)) {
            Set<String> tagSet = new HashSet<>();
            if (StrUtil.isNotBlank(device.getRiskTags())) {
                tagSet.addAll(Arrays.asList(device.getRiskTags().split(",")));
            }
            tagSet.add(tag);
            device.setRiskTags(String.join(",", tagSet));
        }

        if (newScore >= ABNORMAL_THRESHOLD && (device.getStatus() == null || device.getStatus() == 1)) {
            device.setStatus(0);
            log.warn("设备风险评分超过阈值，标记为异常，deviceId：{}，评分：{}", deviceId, newScore);
        } else if (newScore < ABNORMAL_THRESHOLD && device.getStatus() != null && device.getStatus() == 0) {
            device.setStatus(1);
            log.info("设备风险评分低于阈值，恢复为正常，deviceId：{}，评分：{}", deviceId, newScore);
        }

        this.updateById(device);
        evictDeviceCache(deviceId);
        log.info("更新设备风险评分，deviceId：{}，评分变化：{}，当前评分：{}", deviceId, scoreDelta, newScore);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordDeviceRequest(String deviceId, String ip, String userIdentity, String merchantNo) {
        if (StrUtil.isBlank(deviceId)) {
            return;
        }

        RiskDeviceFingerprint device = this.getOrCreateDevice(deviceId, null);
        if (device == null) {
            return;
        }

        device.setTotalRequestCount(device.getTotalRequestCount() != null ? device.getTotalRequestCount() + 1 : 1L);
        device.setLastSeenIp(ip);
        device.setLastSeenTime(LocalDateTime.now());

        if (StrUtil.isNotBlank(userIdentity) && !userIdentity.equals(device.getUserIdentity())) {
            device.setUserIdentity(userIdentity);
        }
        if (StrUtil.isNotBlank(merchantNo) && !merchantNo.equals(device.getMerchantNo())) {
            device.setMerchantNo(merchantNo);
        }

        this.updateById(device);
        evictDeviceCache(deviceId);
    }

    @Override
    public int getDeviceRiskScore(String deviceId) {
        if (StrUtil.isBlank(deviceId)) {
            return 0;
        }
        RiskDeviceFingerprint device = this.getOrCreateDevice(deviceId, null);
        return device != null && device.getRiskScore() != null ? device.getRiskScore() : 0;
    }

    @Override
    public IPage<RiskDeviceVO> listPage(Long current, Long size, Map<String, Object> params) {
        LambdaQueryWrapper<RiskDeviceFingerprint> wrapper = new LambdaQueryWrapper<>();

        if (params != null) {
            if (params.get("deviceId") != null && StrUtil.isNotBlank(params.get("deviceId").toString())) {
                wrapper.like(RiskDeviceFingerprint::getDeviceId, params.get("deviceId").toString());
            }
            if (params.get("deviceType") != null && StrUtil.isNotBlank(params.get("deviceType").toString())) {
                wrapper.eq(RiskDeviceFingerprint::getDeviceType, params.get("deviceType").toString());
            }
            if (params.get("status") != null) {
                wrapper.eq(RiskDeviceFingerprint::getStatus, Integer.parseInt(params.get("status").toString()));
            }
            if (params.get("riskScoreMin") != null) {
                wrapper.ge(RiskDeviceFingerprint::getRiskScore, Integer.parseInt(params.get("riskScoreMin").toString()));
            }
            if (params.get("riskScoreMax") != null) {
                wrapper.le(RiskDeviceFingerprint::getRiskScore, Integer.parseInt(params.get("riskScoreMax").toString()));
            }
            if (params.get("userIdentity") != null && StrUtil.isNotBlank(params.get("userIdentity").toString())) {
                wrapper.eq(RiskDeviceFingerprint::getUserIdentity, params.get("userIdentity").toString());
            }
            if (params.get("merchantNo") != null && StrUtil.isNotBlank(params.get("merchantNo").toString())) {
                wrapper.eq(RiskDeviceFingerprint::getMerchantNo, params.get("merchantNo").toString());
            }
        }

        wrapper.orderByDesc(RiskDeviceFingerprint::getLastSeenTime);

        IPage<RiskDeviceFingerprint> page = this.page(new Page<>(current, size), wrapper);
        return page.convert(this::convertToVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markDeviceAbnormal(String deviceId, String reason) {
        if (StrUtil.isBlank(deviceId)) {
            throw new BusinessException("设备ID不能为空");
        }

        RiskDeviceFingerprint device = this.getOne(new LambdaQueryWrapper<RiskDeviceFingerprint>()
                .eq(RiskDeviceFingerprint::getDeviceId, deviceId));
        if (device == null) {
            throw new BusinessException("设备不存在");
        }

        device.setStatus(0);
        if (StrUtil.isNotBlank(reason)) {
            Set<String> tagSet = new HashSet<>();
            if (StrUtil.isNotBlank(device.getRiskTags())) {
                tagSet.addAll(Arrays.asList(device.getRiskTags().split(",")));
            }
            tagSet.add("ABNORMAL:" + reason);
            device.setRiskTags(String.join(",", tagSet));
        }
        if (device.getRiskScore() == null || device.getRiskScore() < ABNORMAL_THRESHOLD) {
            device.setRiskScore(ABNORMAL_THRESHOLD);
        }

        this.updateById(device);
        evictDeviceCache(deviceId);
        log.info("标记设备异常，deviceId：{}，原因：{}", deviceId, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markDeviceNormal(String deviceId) {
        if (StrUtil.isBlank(deviceId)) {
            throw new BusinessException("设备ID不能为空");
        }

        RiskDeviceFingerprint device = this.getOne(new LambdaQueryWrapper<RiskDeviceFingerprint>()
                .eq(RiskDeviceFingerprint::getDeviceId, deviceId));
        if (device == null) {
            throw new BusinessException("设备不存在");
        }

        device.setStatus(1);
        if (device.getRiskScore() != null && device.getRiskScore() >= ABNORMAL_THRESHOLD) {
            device.setRiskScore(ABNORMAL_THRESHOLD - 1);
        }

        this.updateById(device);
        evictDeviceCache(deviceId);
        log.info("标记设备正常，deviceId：{}", deviceId);
    }

    private String getDeviceInfoValue(Map<String, String> deviceInfo, String key) {
        if (deviceInfo == null || key == null) {
            return null;
        }
        return deviceInfo.get(key);
    }

    private void evictDeviceCache(String deviceId) {
        String cacheKey = DEVICE_KEY_PREFIX + deviceId;
        redisTemplate.delete(cacheKey);
    }

    private RiskDeviceVO convertToVO(RiskDeviceFingerprint entity) {
        RiskDeviceVO vo = new RiskDeviceVO();
        vo.setId(entity.getId());
        vo.setDeviceId(entity.getDeviceId());
        vo.setDeviceType(entity.getDeviceType());
        vo.setOsType(entity.getOsType());
        vo.setOsVersion(entity.getOsVersion());
        vo.setBrowserType(entity.getBrowserType());
        vo.setBrowserVersion(entity.getBrowserVersion());
        vo.setAppVersion(entity.getAppVersion());
        vo.setScreenResolution(entity.getScreenResolution());
        vo.setLanguage(entity.getLanguage());
        vo.setTimezone(entity.getTimezone());
        vo.setUserAgent(entity.getUserAgent());
        vo.setUserIdentity(entity.getUserIdentity());
        vo.setMerchantNo(entity.getMerchantNo());
        vo.setFirstSeenIp(entity.getFirstSeenIp());
        vo.setLastSeenIp(entity.getLastSeenIp());
        vo.setFirstSeenTime(entity.getFirstSeenTime());
        vo.setLastSeenTime(entity.getLastSeenTime());
        vo.setTotalRequestCount(entity.getTotalRequestCount());
        vo.setRiskScore(entity.getRiskScore());
        vo.setRiskTags(entity.getRiskTags());
        vo.setStatus(entity.getStatus());
        vo.setStatusDesc(entity.getStatus() != null && entity.getStatus() == 1 ? "正常" : "异常");
        vo.setExtraInfo(entity.getExtraInfo());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
