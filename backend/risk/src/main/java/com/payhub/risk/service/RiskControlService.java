package com.payhub.risk.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.risk.dto.RiskCheckRequest;
import com.payhub.risk.dto.RiskCheckResult;
import com.payhub.risk.dto.RiskLogVO;
import com.payhub.risk.entity.RiskControlLog;

import java.util.Map;

public interface RiskControlService extends IService<RiskControlLog> {

    RiskCheckResult checkRisk(RiskCheckRequest request);

    IPage<RiskLogVO> listRiskLogs(Long current, Long size, Map<String, Object> params);

    void addIpToBlacklist(String ip, String reason);

    void removeIpFromBlacklist(String ip);

    boolean isIpInBlacklist(String ip);

    Map<String, Object> getDashboardStats();

    RiskLogVO getRiskLogById(Long id);
}
