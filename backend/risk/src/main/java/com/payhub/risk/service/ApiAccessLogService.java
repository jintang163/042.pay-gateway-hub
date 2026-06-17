package com.payhub.risk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.risk.dto.ApiStatsVO;
import com.payhub.risk.entity.ApiAccessLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ApiAccessLogService extends IService<ApiAccessLog> {

    void recordAccessLog(ApiAccessLog log);

    List<ApiStatsVO> getApiStats(LocalDateTime startTime, LocalDateTime endTime);

    List<Map<String, Object>> getTopMerchants(LocalDateTime startTime, LocalDateTime endTime, Integer limit);

    Map<String, Object> getOverviewStats(LocalDateTime startTime, LocalDateTime endTime);
}
