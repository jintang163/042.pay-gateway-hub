package com.payhub.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payhub.marketing.entity.AdClickLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDate;

@Mapper
public interface AdClickLogMapper extends BaseMapper<AdClickLog> {

    @Update("UPDATE ad_stats_daily SET impression_count = impression_count + 1 WHERE stats_date = #{statsDate} AND ad_code = #{adCode}")
    int incrImpression(@Param("statsDate") LocalDate statsDate, @Param("adCode") String adCode);
}
