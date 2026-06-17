package com.payhub.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payhub.risk.entity.ApiAccessLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ApiAccessLogMapper extends BaseMapper<ApiAccessLog> {

    @Select("SELECT api_path, COUNT(*) as call_count, " +
            "SUM(CASE WHEN http_status = 200 THEN 1 ELSE 0 END) as success_count, " +
            "AVG(response_time) as avg_response_time, " +
            "MIN(response_time) as min_response_time, " +
            "MAX(response_time) as max_response_time " +
            "FROM api_access_log " +
            "WHERE access_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY api_path")
    List<Map<String, Object>> selectApiStats(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Select("SELECT merchant_no, COUNT(*) as call_count, " +
            "SUM(CASE WHEN http_status = 200 THEN 1 ELSE 0 END) as success_count " +
            "FROM api_access_log " +
            "WHERE access_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY merchant_no " +
            "ORDER BY call_count DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectTopMerchants(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("limit") Integer limit);
}
