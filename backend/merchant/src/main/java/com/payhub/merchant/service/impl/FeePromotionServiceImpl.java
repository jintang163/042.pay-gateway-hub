package com.payhub.merchant.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.context.CurrentUserContext;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.merchant.dto.*;
import com.payhub.merchant.entity.FeePromotion;
import com.payhub.merchant.entity.FeePromotionMerchant;
import com.payhub.merchant.entity.MerchantInfo;
import com.payhub.merchant.enums.*;
import com.payhub.merchant.mapper.FeePromotionMapper;
import com.payhub.merchant.mapper.FeePromotionMerchantMapper;
import com.payhub.merchant.mapper.MerchantInfoMapper;
import com.payhub.merchant.service.FeePromotionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FeePromotionServiceImpl extends ServiceImpl<FeePromotionMapper, FeePromotion> implements FeePromotionService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Autowired
    private FeePromotionMerchantMapper promotionMerchantMapper;

    @Autowired
    private MerchantInfoMapper merchantInfoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePromotion(FeePromotionSaveRequest request) {
        validatePromotionRequest(request);

        FeePromotion promotion;
        if (request.getId() != null) {
            promotion = this.getById(request.getId());
            if (promotion == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
            }
            if (FeePromotionStatusEnum.ONGOING.getCode().equals(promotion.getStatus())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "进行中的活动不允许修改");
            }
        } else {
            promotion = new FeePromotion();
            promotion.setPromotionNo("FP" + System.currentTimeMillis() + RandomUtil.randomNumbers(4));
            promotion.setUsedQuota(0);
        }

        BeanUtil.copyProperties(request, promotion);

        if (request.getStartTime() == null && request.getEndTime() == null && request.getDurationDays() != null) {
            promotion.setStartTime(LocalDateTime.now());
            promotion.setEndTime(LocalDateTime.now().plusDays(request.getDurationDays()));
        }

        if (promotion.getStartTime() != null && promotion.getEndTime() != null) {
            if (promotion.getStartTime().isAfter(promotion.getEndTime())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "活动开始时间不能晚于结束时间");
            }
            if (promotion.getEndTime().isBefore(LocalDateTime.now())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "活动结束时间不能早于当前时间");
            }
        }

        promotion.setOperatorId(CurrentUserContext.getCurrentUserId());
        promotion.setOperatorName(CurrentUserContext.getCurrentUsername());

        if (promotion.getStatus() == null) {
            promotion.setStatus(FeePromotionStatusEnum.DRAFT.getCode());
        }

        this.saveOrUpdate(promotion);

        if (FeePromotionTargetTypeEnum.DESIGNATED.getCode().equals(request.getTargetType())
                && request.getMerchantNoList() != null && !request.getMerchantNoList().isEmpty()) {
            bindMerchantsToPromotion(promotion.getPromotionNo(), request.getMerchantNoList(), promotion.getStartTime(), promotion.getEndTime());
        }

        log.info("费率优惠活动保存成功: promotionNo={}, promotionName={}", promotion.getPromotionNo(), promotion.getPromotionName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id) {
        FeePromotion promotion = this.getById(id);
        if (promotion == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }

        Integer currentStatus = promotion.getStatus();
        if (FeePromotionStatusEnum.DRAFT.getCode().equals(currentStatus)) {
            promotion.setStatus(FeePromotionStatusEnum.NOT_STARTED.getCode());
            log.info("活动发布: promotionNo={}", promotion.getPromotionNo());
        } else if (FeePromotionStatusEnum.NOT_STARTED.getCode().equals(currentStatus)
                || FeePromotionStatusEnum.ONGOING.getCode().equals(currentStatus)) {
            promotion.setStatus(FeePromotionStatusEnum.DISABLED.getCode());
            log.info("活动停用: promotionNo={}", promotion.getPromotionNo());
        } else if (FeePromotionStatusEnum.DISABLED.getCode().equals(currentStatus)) {
            LocalDateTime now = LocalDateTime.now();
            if (promotion.getEndTime() != null && promotion.getEndTime().isBefore(now)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "活动已过期，无法重新启用");
            }
            promotion.setStatus(promotion.getStartTime() != null && promotion.getStartTime().isBefore(now)
                    ? FeePromotionStatusEnum.ONGOING.getCode()
                    : FeePromotionStatusEnum.NOT_STARTED.getCode());
            log.info("活动重新启用: promotionNo={}", promotion.getPromotionNo());
        } else {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前状态不允许切换");
        }

        this.updateById(promotion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePromotion(Long id) {
        FeePromotion promotion = this.getById(id);
        if (promotion == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        if (FeePromotionStatusEnum.ONGOING.getCode().equals(promotion.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "进行中的活动不允许删除");
        }
        this.removeById(id);
        log.info("费率优惠活动删除成功: promotionNo={}", promotion.getPromotionNo());
    }

    @Override
    public FeePromotionVO getByPromotionNo(String promotionNo) {
        FeePromotion promotion = baseMapper.selectByPromotionNo(promotionNo);
        if (promotion == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "活动不存在");
        }
        return convertToVO(promotion);
    }

    @Override
    public IPage<FeePromotionVO> listPage(int current, int size, Map<String, Object> params) {
        Page<FeePromotion> page = new Page<>(current, size);
        IPage<FeePromotion> promotionPage = baseMapper.selectPageList(page, params);
        return promotionPage.convert(this::convertToVO);
    }

    @Override
    public List<MerchantFeePromotionVO> listMerchantPromotions(String merchantNo) {
        MerchantInfo merchant = getMerchantInfo(merchantNo);
        if (merchant == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商户不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        List<FeePromotion> promotions = baseMapper.selectAvailablePromotions(
                merchantNo, merchant.getIndustryCode(), now);
        List<FeePromotionMerchant> merchantPromotions = promotionMerchantMapper.selectByMerchantNo(merchantNo, now);

        Map<String, FeePromotionMerchant> merchantPromotionMap = merchantPromotions.stream()
                .collect(Collectors.toMap(FeePromotionMerchant::getPromotionNo, p -> p));

        List<MerchantFeePromotionVO> result = new ArrayList<>();
        for (FeePromotion promotion : promotions) {
            MerchantFeePromotionVO vo = convertToMerchantVO(promotion, merchantPromotionMap.get(promotion.getPromotionNo()));
            if (vo != null) {
                result.add(vo);
            }
        }

        return result;
    }

    @Override
    public MerchantFeePromotionVO getCurrentBestPromotion(String merchantNo) {
        List<MerchantFeePromotionVO> promotions = listMerchantPromotions(merchantNo);
        if (promotions.isEmpty()) {
            return null;
        }
        return promotions.stream()
                .max(Comparator.comparingInt(this::getPromotionPriority))
                .orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FeePromotionCalcResult calculatePromotionFee(String merchantNo, BigDecimal originalFee, BigDecimal originalFeeRate, BigDecimal amount) {
        FeePromotionCalcResult result = new FeePromotionCalcResult();
        result.setOriginalFee(originalFee);
        result.setOriginalFeeRate(originalFeeRate);
        result.setHasPromotion(false);
        result.setActualFee(originalFee);
        result.setSavedAmount(BigDecimal.ZERO);

        MerchantFeePromotionVO bestPromotion = getCurrentBestPromotion(merchantNo);
        if (bestPromotion == null) {
            result.setCalcDetail("无可用费率优惠活动");
            return result;
        }

        FeePromotionFeeTypeEnum feeType = FeePromotionFeeTypeEnum.getByCode(bestPromotion.getFeeType());
        if (feeType == null) {
            result.setCalcDetail("优惠类型错误");
            return result;
        }

        BigDecimal discountFee = originalFee;
        String calcDetail;

        switch (feeType) {
            case ZERO_FEE:
                discountFee = BigDecimal.ZERO;
                calcDetail = String.format("参与活动[%s]，0手续费", bestPromotion.getPromotionName());
                break;
            case RATE_DISCOUNT:
                if (bestPromotion.getDiscountFeeRate() != null) {
                    discountFee = amount.multiply(bestPromotion.getDiscountFeeRate())
                            .divide(HUNDRED, 2, RoundingMode.HALF_UP);
                    if (bestPromotion.getMaxDiscountAmount() != null) {
                        BigDecimal maxSaving = originalFee.subtract(bestPromotion.getMaxDiscountAmount());
                        if (discountFee.compareTo(maxSaving) < 0) {
                            discountFee = maxSaving;
                            calcDetail = String.format("参与活动[%s]，费率%s%%，但已达单笔最高优惠%s元",
                                    bestPromotion.getPromotionName(), bestPromotion.getDiscountFeeRate(), bestPromotion.getMaxDiscountAmount());
                        } else {
                            calcDetail = String.format("参与活动[%s]，原费率%s%%，优惠费率%s%%",
                                    bestPromotion.getPromotionName(), originalFeeRate, bestPromotion.getDiscountFeeRate());
                        }
                    } else {
                        calcDetail = String.format("参与活动[%s]，原费率%s%%，优惠费率%s%%",
                                bestPromotion.getPromotionName(), originalFeeRate, bestPromotion.getDiscountFeeRate());
                    }
                } else {
                    result.setCalcDetail("优惠费率配置错误");
                    return result;
                }
                break;
            case FIXED_FEE:
                if (bestPromotion.getFixedFeeAmount() != null) {
                    discountFee = bestPromotion.getFixedFeeAmount();
                    calcDetail = String.format("参与活动[%s]，固定手续费%s元",
                            bestPromotion.getPromotionName(), bestPromotion.getFixedFeeAmount());
                } else {
                    result.setCalcDetail("固定手续费配置错误");
                    return result;
                }
                break;
            default:
                result.setCalcDetail("未知优惠类型");
                return result;
        }

        if (discountFee.compareTo(BigDecimal.ZERO) < 0) {
            discountFee = BigDecimal.ZERO;
        }

        BigDecimal savedAmount = originalFee.subtract(discountFee);
        if (savedAmount.compareTo(BigDecimal.ZERO) < 0) {
            savedAmount = BigDecimal.ZERO;
            discountFee = originalFee;
        }

        result.setHasPromotion(true);
        result.setPromotionNo(bestPromotion.getPromotionNo());
        result.setPromotionName(bestPromotion.getPromotionName());
        result.setFeeType(bestPromotion.getFeeType());
        result.setFeeTypeDesc(bestPromotion.getFeeTypeDesc());
        result.setDiscountFeeRate(bestPromotion.getDiscountFeeRate());
        result.setDiscountFee(discountFee);
        result.setActualFee(discountFee);
        result.setSavedAmount(savedAmount);
        result.setCalcDetail(calcDetail);

        incrementPromotionUsage(merchantNo, bestPromotion.getPromotionNo(), savedAmount);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindNewMerchantPromotion(String merchantNo, String merchantName, String industryCode) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<FeePromotion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeePromotion::getTargetType, FeePromotionTargetTypeEnum.NEW_REGISTER.getCode())
                .eq(FeePromotion::getStatus, FeePromotionStatusEnum.ONGOING.getCode())
                .le(FeePromotion::getStartTime, now)
                .ge(FeePromotion::getEndTime, now)
                .and(w -> w.isNull(FeePromotion::getTotalQuota).or().lt(FeePromotion::getUsedQuota, FeePromotion::getTotalQuota));

        List<FeePromotion> promotions = this.list(wrapper);
        for (FeePromotion promotion : promotions) {
            LocalDateTime startTime = now;
            LocalDateTime endTime = promotion.getDurationDays() != null
                    ? startTime.plusDays(promotion.getDurationDays())
                    : promotion.getEndTime();

            FeePromotionMerchant existing = promotionMerchantMapper.selectByPromotionAndMerchant(
                    promotion.getPromotionNo(), merchantNo);
            if (existing == null) {
                FeePromotionMerchant pm = new FeePromotionMerchant();
                pm.setPromotionNo(promotion.getPromotionNo());
                pm.setMerchantNo(merchantNo);
                pm.setMerchantName(merchantName);
                pm.setStartTime(startTime);
                pm.setEndTime(endTime);
                pm.setTotalQuota(promotion.getPerMerchantQuota());
                pm.setUsedQuota(0);
                pm.setTotalDiscountAmount(BigDecimal.ZERO);
                pm.setStatus(1);
                pm.setBindSource("NEW_MERCHANT");
                pm.setRemark("新商户注册自动绑定");
                promotionMerchantMapper.insert(pm);
                log.info("新商户自动绑定费率优惠活动: merchantNo={}, promotionNo={}", merchantNo, promotion.getPromotionNo());
            }
        }
    }

    private void validatePromotionRequest(FeePromotionSaveRequest request) {
        FeePromotionFeeTypeEnum feeType = FeePromotionFeeTypeEnum.getByCode(request.getFeeType());
        if (feeType == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的优惠类型");
        }

        if (FeePromotionFeeTypeEnum.RATE_DISCOUNT.getCode().equals(request.getFeeType())) {
            if (request.getDiscountFeeRate() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "费率折扣类型必须设置优惠费率");
            }
            if (request.getDiscountFeeRate().compareTo(BigDecimal.ZERO) < 0
                    || request.getDiscountFeeRate().compareTo(HUNDRED) > 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "优惠费率必须在0到100之间");
            }
        }

        if (FeePromotionFeeTypeEnum.FIXED_FEE.getCode().equals(request.getFeeType())) {
            if (request.getFixedFeeAmount() == null || request.getFixedFeeAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "固定手续费类型必须设置有效的手续费金额");
            }
        }
    }

    private void bindMerchantsToPromotion(String promotionNo, List<String> merchantNoList,
                                           LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<FeePromotionMerchant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FeePromotionMerchant::getPromotionNo, promotionNo);
        promotionMerchantMapper.delete(wrapper);

        LocalDateTime now = LocalDateTime.now();
        for (String merchantNo : merchantNoList) {
            MerchantInfo merchant = getMerchantInfo(merchantNo);
            if (merchant != null) {
                FeePromotionMerchant pm = new FeePromotionMerchant();
                pm.setPromotionNo(promotionNo);
                pm.setMerchantNo(merchantNo);
                pm.setMerchantName(merchant.getMerchantName());
                pm.setStartTime(startTime != null ? startTime : now);
                pm.setEndTime(endTime);
                pm.setUsedQuota(0);
                pm.setTotalDiscountAmount(BigDecimal.ZERO);
                pm.setStatus(1);
                pm.setBindSource("MANUAL");
                promotionMerchantMapper.insert(pm);
            }
        }
        log.info("活动商户绑定完成: promotionNo={}, merchantCount={}", promotionNo, merchantNoList.size());
    }

    private void incrementPromotionUsage(String merchantNo, String promotionNo, BigDecimal discountAmount) {
        FeePromotion promotion = baseMapper.selectByPromotionNo(promotionNo);
        if (promotion != null && promotion.getUsedQuota() != null) {
            promotion.setUsedQuota(promotion.getUsedQuota() + 1);
            this.updateById(promotion);
        }

        FeePromotionMerchant pm = promotionMerchantMapper.selectByPromotionAndMerchant(promotionNo, merchantNo);
        if (pm != null) {
            promotionMerchantMapper.incrementUsedQuota(pm.getId(), discountAmount);
        }
    }

    private int getPromotionPriority(MerchantFeePromotionVO vo) {
        FeePromotionFeeTypeEnum feeType = FeePromotionFeeTypeEnum.getByCode(vo.getFeeType());
        if (feeType == null) return 0;
        switch (feeType) {
            case ZERO_FEE: return 3;
            case FIXED_FEE: return 2;
            case RATE_DISCOUNT: return 1;
            default: return 0;
        }
    }

    private FeePromotionVO convertToVO(FeePromotion promotion) {
        FeePromotionVO vo = BeanUtil.copyProperties(promotion, FeePromotionVO.class);
        FeePromotionTypeEnum typeEnum = FeePromotionTypeEnum.getByCode(promotion.getPromotionType());
        vo.setPromotionTypeDesc(typeEnum != null ? typeEnum.getDesc() : "");
        FeePromotionTargetTypeEnum targetEnum = FeePromotionTargetTypeEnum.getByCode(promotion.getTargetType());
        vo.setTargetTypeDesc(targetEnum != null ? targetEnum.getDesc() : "");
        FeePromotionFeeTypeEnum feeTypeEnum = FeePromotionFeeTypeEnum.getByCode(promotion.getFeeType());
        vo.setFeeTypeDesc(feeTypeEnum != null ? feeTypeEnum.getDesc() : "");
        FeePromotionStatusEnum statusEnum = FeePromotionStatusEnum.getByCode(promotion.getStatus());
        vo.setStatusDesc(statusEnum != null ? statusEnum.getDesc() : "");
        return vo;
    }

    private MerchantFeePromotionVO convertToMerchantVO(FeePromotion promotion, FeePromotionMerchant merchantPromotion) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = merchantPromotion != null && merchantPromotion.getStartTime() != null
                ? merchantPromotion.getStartTime() : promotion.getStartTime();
        LocalDateTime endTime = merchantPromotion != null && merchantPromotion.getEndTime() != null
                ? merchantPromotion.getEndTime() : promotion.getEndTime();

        if (startTime == null || endTime == null) {
            return null;
        }
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return null;
        }

        MerchantFeePromotionVO vo = new MerchantFeePromotionVO();
        vo.setId(promotion.getId());
        vo.setPromotionNo(promotion.getPromotionNo());
        vo.setPromotionName(promotion.getPromotionName());
        vo.setPromotionDesc(promotion.getPromotionDesc());
        vo.setPromotionType(promotion.getPromotionType());
        FeePromotionTypeEnum typeEnum = FeePromotionTypeEnum.getByCode(promotion.getPromotionType());
        vo.setPromotionTypeDesc(typeEnum != null ? typeEnum.getDesc() : "");
        vo.setFeeType(promotion.getFeeType());
        FeePromotionFeeTypeEnum feeTypeEnum = FeePromotionFeeTypeEnum.getByCode(promotion.getFeeType());
        vo.setFeeTypeDesc(feeTypeEnum != null ? feeTypeEnum.getDesc() : "");
        vo.setDiscountFeeRate(promotion.getDiscountFeeRate());
        vo.setFixedFeeAmount(promotion.getFixedFeeAmount());
        vo.setMaxDiscountAmount(promotion.getMaxDiscountAmount());
        vo.setStartTime(startTime);
        vo.setEndTime(endTime);
        vo.setTotalQuota(merchantPromotion != null ? merchantPromotion.getTotalQuota() : promotion.getTotalQuota());
        vo.setUsedQuota(merchantPromotion != null ? merchantPromotion.getUsedQuota() : 0);
        vo.setTotalDiscountAmount(merchantPromotion != null ? merchantPromotion.getTotalDiscountAmount() : BigDecimal.ZERO);
        vo.setStatus(promotion.getStatus());
        FeePromotionStatusEnum statusEnum = FeePromotionStatusEnum.getByCode(promotion.getStatus());
        vo.setStatusDesc(statusEnum != null ? statusEnum.getDesc() : "");
        vo.setRemark(promotion.getRemark());
        vo.setCreatedAt(promotion.getCreatedAt());

        long remainingSeconds = Duration.between(now, endTime).getSeconds();
        vo.setRemainingSeconds(remainingSeconds);
        vo.setCountdownText(formatCountdown(remainingSeconds));

        return vo;
    }

    private String formatCountdown(long seconds) {
        if (seconds <= 0) {
            return "活动已结束";
        }
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (days > 0) {
            return String.format("剩余 %d天%d小时", days, hours);
        } else if (hours > 0) {
            return String.format("剩余 %d小时%d分钟", hours, minutes);
        } else if (minutes > 0) {
            return String.format("剩余 %d分钟%d秒", minutes, secs);
        } else {
            return String.format("剩余 %d秒", secs);
        }
    }

    private MerchantInfo getMerchantInfo(String merchantNo) {
        try {
            LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MerchantInfo::getMerchantNo, merchantNo);
            return merchantInfoMapper.selectOne(wrapper);
        } catch (Exception e) {
            log.warn("获取商户信息失败: merchantNo={}", merchantNo, e);
            return null;
        }
    }
}
