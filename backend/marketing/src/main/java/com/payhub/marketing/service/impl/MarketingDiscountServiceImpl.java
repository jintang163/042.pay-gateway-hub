package com.payhub.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.payhub.common.exception.BusinessException;
import com.payhub.marketing.dto.CouponDiscountCalcResult;
import com.payhub.marketing.entity.Activity;
import com.payhub.marketing.entity.Coupon;
import com.payhub.marketing.entity.CouponUseLog;
import com.payhub.marketing.entity.PayLink;
import com.payhub.marketing.enums.ActivityStatusEnum;
import com.payhub.marketing.enums.ActivityTypeEnum;
import com.payhub.marketing.enums.CouponStatusEnum;
import com.payhub.marketing.enums.CouponTypeEnum;
import com.payhub.marketing.mapper.ActivityMapper;
import com.payhub.marketing.mapper.CouponMapper;
import com.payhub.marketing.mapper.CouponUseLogMapper;
import com.payhub.marketing.mapper.PayLinkMapper;
import com.payhub.marketing.service.MarketingDiscountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
public class MarketingDiscountServiceImpl implements MarketingDiscountService {

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private CouponUseLogMapper couponUseLogMapper;

    @Autowired
    private PayLinkMapper payLinkMapper;

    @Override
    public CouponDiscountCalcResult calcCouponDiscount(String couponCode, String merchantNo, BigDecimal orderAmount) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getCouponCode, couponCode);
        Coupon coupon = couponMapper.selectOne(wrapper);
        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (merchantNo != null && !merchantNo.equals(coupon.getMerchantNo())) {
            throw new BusinessException("优惠券不属于该商户");
        }
        if (!CouponStatusEnum.ACTIVE.getCode().equals(coupon.getStatus())) {
            throw new BusinessException("优惠券未在生效中");
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartTime() != null && now.isBefore(coupon.getStartTime())) {
            throw new BusinessException("优惠券尚未生效");
        }
        if (coupon.getEndTime() != null && now.isAfter(coupon.getEndTime())) {
            throw new BusinessException("优惠券已过期");
        }
        if (coupon.getUsedCount() >= coupon.getTotalQuantity()) {
            throw new BusinessException("优惠券已被领完");
        }
        if (coupon.getMinOrderAmount() != null && orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BusinessException("订单金额不满足最低消费" + coupon.getMinOrderAmount() + "元");
        }

        BigDecimal discountAmount;
        String calcDetail;
        if (CouponTypeEnum.FIXED_DISCOUNT.getCode().equals(coupon.getCouponType())) {
            discountAmount = coupon.getDiscountValue();
            if (discountAmount.compareTo(orderAmount) > 0) {
                discountAmount = orderAmount;
            }
            calcDetail = "固定抵扣" + coupon.getDiscountValue() + "元";
        } else if (CouponTypeEnum.PERCENT_DISCOUNT.getCode().equals(coupon.getCouponType())) {
            BigDecimal rate = coupon.getDiscountValue().divide(BigDecimal.valueOf(10), 4, RoundingMode.HALF_UP);
            discountAmount = orderAmount.multiply(BigDecimal.ONE.subtract(rate)).setScale(2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscount() != null && discountAmount.compareTo(coupon.getMaxDiscount()) > 0) {
                discountAmount = coupon.getMaxDiscount();
                calcDetail = rate.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP) + "折抵扣，上限" + coupon.getMaxDiscount() + "元";
            } else {
                calcDetail = rate.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP) + "折抵扣";
            }
        } else {
            throw new BusinessException("不支持的优惠券类型");
        }

        if (discountAmount.compareTo(orderAmount) > 0) {
            discountAmount = orderAmount;
        }

        CouponDiscountCalcResult result = new CouponDiscountCalcResult();
        result.setCouponCode(coupon.getCouponCode());
        result.setCouponName(coupon.getCouponName());
        result.setCouponType(coupon.getCouponType());
        CouponTypeEnum typeEnum = CouponTypeEnum.getByCode(coupon.getCouponType());
        result.setCouponTypeDesc(typeEnum != null ? typeEnum.getDesc() : "未知");
        result.setOrderAmount(orderAmount);
        result.setDiscountAmount(discountAmount);
        result.setActualAmount(orderAmount.subtract(discountAmount));
        result.setCalcDetail(calcDetail);
        return result;
    }

    @Override
    public BigDecimal calcActivityDiscount(String activityCode, String merchantNo, BigDecimal orderAmount) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getActivityCode, activityCode);
        Activity activity = activityMapper.selectOne(wrapper);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        if (merchantNo != null && !merchantNo.equals(activity.getMerchantNo())) {
            throw new BusinessException("活动不属于该商户");
        }
        if (!ActivityStatusEnum.ACTIVE.getCode().equals(activity.getStatus())) {
            throw new BusinessException("活动未在进行中");
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.getStartTime() != null && now.isBefore(activity.getStartTime())) {
            throw new BusinessException("活动尚未开始");
        }
        if (activity.getEndTime() != null && now.isAfter(activity.getEndTime())) {
            throw new BusinessException("活动已结束");
        }
        if (activity.getThresholdAmount() != null && orderAmount.compareTo(activity.getThresholdAmount()) < 0) {
            throw new BusinessException("订单金额不满足活动门槛" + activity.getThresholdAmount() + "元");
        }

        BigDecimal discountAmount;
        if (ActivityTypeEnum.FULL_REDUCTION.getCode().equals(activity.getActivityType())) {
            discountAmount = activity.getDiscountAmount();
        } else if (ActivityTypeEnum.DISCOUNT.getCode().equals(activity.getActivityType())) {
            BigDecimal rate = activity.getDiscountRate().divide(BigDecimal.valueOf(10), 4, RoundingMode.HALF_UP);
            discountAmount = orderAmount.multiply(BigDecimal.ONE.subtract(rate)).setScale(2, RoundingMode.HALF_UP);
            if (activity.getMaxDiscount() != null && discountAmount.compareTo(activity.getMaxDiscount()) > 0) {
                discountAmount = activity.getMaxDiscount();
            }
        } else {
            throw new BusinessException("不支持的活动类型");
        }

        if (discountAmount.compareTo(orderAmount) > 0) {
            discountAmount = orderAmount;
        }

        return discountAmount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordCouponUse(String orderNo, String merchantNo, String couponCode,
                                BigDecimal orderAmount, BigDecimal discountAmount, String userId) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getCouponCode, couponCode);
        Coupon coupon = couponMapper.selectOne(wrapper);
        if (coupon == null) {
            log.warn("核销优惠券失败：优惠券不存在, couponCode={}", couponCode);
            return;
        }

        LambdaUpdateWrapper<Coupon> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Coupon::getId, coupon.getId())
                .eq(Coupon::getUsedCount, coupon.getUsedCount())
                .setSql("used_count = used_count + 1");
        int rows = couponMapper.update(null, updateWrapper);
        if (rows == 0) {
            log.warn("优惠券核销乐观锁失败, couponCode={}", couponCode);
        }

        CouponUseLog useLog = new CouponUseLog();
        useLog.setCouponCode(couponCode);
        useLog.setMerchantNo(merchantNo);
        useLog.setOrderNo(orderNo);
        useLog.setUserId(userId);
        useLog.setOrderAmount(orderAmount);
        useLog.setDiscountAmount(discountAmount);
        useLog.setUseType(1);
        useLog.setUsedAt(LocalDateTime.now());
        couponUseLogMapper.insert(useLog);

        log.info("优惠券核销成功, couponCode={}, orderNo={}, discountAmount={}", couponCode, orderNo, discountAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementPayLinkUsed(String linkCode) {
        LambdaQueryWrapper<PayLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PayLink::getLinkCode, linkCode);
        PayLink payLink = payLinkMapper.selectOne(wrapper);
        if (payLink == null) {
            log.warn("支付链接不存在, linkCode={}", linkCode);
            return;
        }

        LambdaUpdateWrapper<PayLink> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PayLink::getId, payLink.getId())
                .eq(PayLink::getUsedCount, payLink.getUsedCount())
                .setSql("used_count = used_count + 1");
        int rows = payLinkMapper.update(null, updateWrapper);
        if (rows == 0) {
            log.warn("支付链接使用次数递增乐观锁失败, linkCode={}", linkCode);
        }

        log.info("支付链接使用次数+1, linkCode={}, usedCount={}", linkCode, payLink.getUsedCount() + 1);
    }
}
