package com.payhub.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payhub.merchant.entity.FeePromotionMerchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FeePromotionMerchantMapper extends BaseMapper<FeePromotionMerchant> {

    List<FeePromotionMerchant> selectByMerchantNo(@Param("merchantNo") String merchantNo, @Param("currentTime") LocalDateTime currentTime);

    FeePromotionMerchant selectByPromotionAndMerchant(@Param("promotionNo") String promotionNo, @Param("merchantNo") String merchantNo);

    int incrementUsedQuota(@Param("id") Long id, @Param("discountAmount") java.math.BigDecimal discountAmount);
}
