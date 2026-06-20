package com.payhub.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.merchant.entity.FeePromotion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface FeePromotionMapper extends BaseMapper<FeePromotion> {

    IPage<FeePromotion> selectPageList(IPage<FeePromotion> page, @Param("params") Map<String, Object> params);

    List<FeePromotion> selectAvailablePromotions(
            @Param("merchantNo") String merchantNo,
            @Param("industryCode") String industryCode,
            @Param("currentTime") LocalDateTime currentTime);

    FeePromotion selectByPromotionNo(@Param("promotionNo") String promotionNo);
}
