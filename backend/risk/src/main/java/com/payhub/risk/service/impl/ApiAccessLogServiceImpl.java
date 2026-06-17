package com.payhub.risk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.risk.dto.ApiStatsVO;
import com.payhub.risk.entity.ApiAccessLog;
import com.payhub.risk.mapper.ApiAccessLogMapper;
import com.payhub.risk.service.ApiAccessLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ApiAccessLogServiceImpl extends ServiceImpl<ApiAccessLogMapper, ApiAccessLog> implements ApiAccessLogService {

    @Autowired
    private ApiAccessLogMapper apiAccessLogMapper;

    @Override
    @Async
    public void recordAccessLog(ApiAccessLog accessLog) {
        try {
            this.save(accessLog);
        } catch (Exception e) {
            log.error("记录API访问日志失败", e);
        }
    }

    @Override
    public List<ApiStatsVO> getApiStats(LocalDateTime startTime, LocalDateTime endTime) {
        List<Map<String, Object>> statsList = apiAccessLogMapper.selectApiStats(startTime, endTime);
        List<ApiStatsVO> result = new ArrayList<>();
        for (Map<String, Object> stats : statsList) {
            ApiStatsVO vo = new ApiStatsVO();
            vo.setApiPath((String) stats.get("api_path"));
            Long callCount = ((Number) stats.get("call_count")).longValue();
            Long successCount = ((Number) stats.get("success_count")).longValue();
            vo.setCallCount(callCount);
            vo.setSuccessCount(successCount);
            vo.setFailCount(callCount - successCount);
            if (callCount > 0) {
                BigDecimal rate = new BigDecimal(successCount)
                        .multiply(new BigDecimal("100"))
                        .divide(new BigDecimal(callCount), 2, RoundingMode.HALF_UP);
                vo.setSuccessRate(rate.doubleValue());
            } else {
                vo.setSuccessRate(0.0);
            }
            Object avgRt = stats.get("avg_response_time");
            Object minRt = stats.get("min_response_time");
            Object maxRt = stats.get("max_response_time");
            vo.setAvgResponseTime(avgRt != null ? ((Number) avgRt).doubleValue() : 0.0);
            vo.setMinResponseTime(minRt != null ? ((Number) minRt).longValue() : 0L);
            vo.setMaxResponseTime(maxRt != null ? ((Number) maxRt).longValue() : 0L);
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getTopMerchants(LocalDateTime startTime, LocalDateTime endTime, Integer limit) {
        return apiAccessLogMapper.selectTopMerchants(startTime, endTime, limit);
    }

    @Override
    public Map<String, Object> getOverviewStats(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<ApiAccessLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(ApiAccessLog::getAccessTime, startTime, endTime);

        Long totalCount = this.count(wrapper);

        LambdaQueryWrapper<ApiAccessLog> successWrapper = new LambdaQueryWrapper<>();
        successWrapper.between(ApiAccessLog::getAccessTime, startTime, endTime)
                .eq(ApiAccessLog::getHttpStatus, 200);
        Long successCount = this.count(successWrapper);

        List<Object> rtList = this.listObjs(new LambdaQueryWrapper<ApiAccessLog>()
                .between(ApiAccessLog::getAccessTime, startTime, endTime)
                .select(ApiAccessLog::getResponseTime)
                .isNotNull(ApiAccessLog::getResponseTime));

        double avgRt = 0.0;
        long maxRt = 0L;
        if (!rtList.isEmpty()) {
            long sum = 0L;
            for (Object obj : rtList) {
                long rt = ((Number) obj).longValue();
                sum += rt;
                maxRt = Math.max(maxRt, rt);
            }
            avgRt = (double) sum / rtList.size();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", totalCount);
        result.put("successCount", successCount);
        result.put("failCount", totalCount - successCount);
        result.put("successRate", totalCount > 0
                ? new BigDecimal(successCount).multiply(new BigDecimal("100"))
                        .divide(new BigDecimal(totalCount), 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0);
        result.put("avgResponseTime", avgRt);
        result.put("maxResponseTime", maxRt);
        return result;
    }
}
