package com.payhub.risk.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.risk.dto.RiskDeviceVO;
import com.payhub.risk.entity.RiskDeviceFingerprint;

import java.util.Map;

public interface RiskDeviceService extends IService<RiskDeviceFingerprint> {

    RiskDeviceFingerprint getOrCreateDevice(String deviceId, Map<String, String> deviceInfo);

    void updateDeviceRisk(String deviceId, int scoreDelta, String tag);

    void recordDeviceRequest(String deviceId, String ip, String userIdentity, String merchantNo);

    int getDeviceRiskScore(String deviceId);

    IPage<RiskDeviceVO> listPage(Long current, Long size, Map<String, Object> params);

    void markDeviceAbnormal(String deviceId, String reason);

    void markDeviceNormal(String deviceId);
}
