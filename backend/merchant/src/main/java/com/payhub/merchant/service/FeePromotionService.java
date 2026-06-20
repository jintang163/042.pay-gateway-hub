package com.payhub.merchant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.merchant.dto.FeePromotionCalcResult;
import com.payhub.merchant.dto.FeePromotionSaveRequest;
import com.payhub.merchant.dto.FeePromotionVO;
import com.payhub.merchant.dto.MerchantFeePromotionVO;
import com.payhub.merchant.entity.FeePromotion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface FeePromotionService extends IService<FeePromotion> {

    void savePromotion(FeePromotionSaveRequest request);

    void toggleStatus(Long id);

    void deletePromotion(Long id);

    FeePromotionVO getByPromotionNo(String promotionNo);

    IPage<FeePromotionVO> listPage(int current, int size, Map<String, Object> params);

    List<MerchantFeePromotionVO> listMerchantPromotions(String merchantNo);

    MerchantFeePromotionVO getCurrentBestPromotion(String merchantNo);

    FeePromotionCalcResult calculatePromotionFee(String merchantNo, BigDecimal originalFee, BigDecimal originalFeeRate, BigDecimal amount);

    void bindNewMerchantPromotion(String merchantNo, String merchantName, String industryCode);

    void refreshPromotionStatus();
}
