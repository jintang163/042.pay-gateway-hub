package com.payhub.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.marketing.dto.CouponDiscountCalcRequest;
import com.payhub.marketing.dto.CouponDiscountCalcResult;
import com.payhub.marketing.dto.CouponSaveRequest;
import com.payhub.marketing.dto.CouponVO;
import com.payhub.marketing.entity.Coupon;
import com.payhub.marketing.enums.CouponStatusEnum;
import com.payhub.marketing.enums.CouponTypeEnum;
import com.payhub.marketing.mapper.CouponMapper;
import com.payhub.marketing.service.CouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
public class CouponServiceImpl implements CouponService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private CouponMapper couponMapper;

    @Override
    public IPage<CouponVO> listPage(Long current, Long size, String merchantNo, Map<String, Object> params) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(merchantNo), Coupon::getMerchantNo, merchantNo);
        if (params != null) {
            wrapper.eq(params.get("couponCode") != null, Coupon::getCouponCode, params.get("couponCode"));
            wrapper.eq(params.get("couponType") != null, Coupon::getCouponType, params.get("couponType"));
            wrapper.eq(params.get("status") != null, Coupon::getStatus, params.get("status"));
            wrapper.like(params.get("couponName") != null, Coupon::getCouponName, params.get("couponName"));
        }
        wrapper.orderByDesc(Coupon::getCreatedAt);
        IPage<Coupon> page = couponMapper.selectPage(new Page<>(current, size), wrapper);
        return page.convert(this::toVO);
    }

    @Override
    public CouponVO getByCouponCode(String couponCode) {
        Coupon coupon = getByCouponCodeEntity(couponCode);
        return toVO(coupon);
    }

    @Override
    public void saveCoupon(CouponSaveRequest request) {
        Coupon coupon;
        if (request.getId() != null) {
            coupon = couponMapper.selectById(request.getId());
            if (coupon == null) {
                throw new BusinessException("优惠券不存在");
            }
            if (coupon.getIssuedCount() > 0) {
                throw new BusinessException("优惠券已发放，不可修改");
            }
        } else {
            coupon = new Coupon();
            coupon.setCouponCode(OrderNoGenerator.generateWithPrefix("CP"));
            coupon.setIssuedCount(0);
            coupon.setUsedCount(0);
            coupon.setStatus(CouponStatusEnum.NOT_STARTED.getCode());
        }
        coupon.setMerchantNo(request.getMerchantNo());
        coupon.setCouponName(request.getCouponName());
        coupon.setCouponType(request.getCouponType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setMaxDiscount(request.getMaxDiscount());
        coupon.setTotalQuantity(request.getTotalQuantity());
        if (StringUtils.hasText(request.getStartTime())) {
            coupon.setStartTime(LocalDateTime.parse(request.getStartTime(), DTF));
        }
        if (StringUtils.hasText(request.getEndTime())) {
            coupon.setEndTime(LocalDateTime.parse(request.getEndTime(), DTF));
        }
        coupon.setRemark(request.getRemark());
        if (request.getId() != null) {
            couponMapper.updateById(coupon);
        } else {
            couponMapper.insert(coupon);
        }
    }

    @Override
    public void toggleStatus(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        if (CouponStatusEnum.ACTIVE.getCode().equals(coupon.getStatus())) {
            coupon.setStatus(CouponStatusEnum.PAUSED.getCode());
        } else if (CouponStatusEnum.PAUSED.getCode().equals(coupon.getStatus()) ||
                CouponStatusEnum.NOT_STARTED.getCode().equals(coupon.getStatus())) {
            if (coupon.getIssuedCount() >= coupon.getTotalQuantity()) {
                coupon.setStatus(CouponStatusEnum.EXHAUSTED.getCode());
            } else if (coupon.getEndTime() != null && coupon.getEndTime().isBefore(LocalDateTime.now())) {
                coupon.setStatus(CouponStatusEnum.EXPIRED.getCode());
            } else {
                coupon.setStatus(CouponStatusEnum.ACTIVE.getCode());
            }
        } else {
            throw new BusinessException("当前状态不允许切换");
        }
        couponMapper.updateById(coupon);
    }

    @Override
    public void deleteCoupon(Long id) {
        Coupon coupon = couponMapper.selectById(id);
        if (coupon != null && coupon.getIssuedCount() > 0) {
            throw new BusinessException("已发放的优惠券不可删除");
        }
        couponMapper.deleteById(id);
    }

    @Override
    public CouponDiscountCalcResult calculateDiscount(CouponDiscountCalcRequest request) {
        Coupon coupon = getByCouponCodeEntity(request.getCouponCode());
        if (request.getMerchantNo() != null && !request.getMerchantNo().equals(coupon.getMerchantNo())) {
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
        if (coupon.getMinOrderAmount() != null && request.getOrderAmount().compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new BusinessException("订单金额不满足最低消费" + coupon.getMinOrderAmount() + "元");
        }

        BigDecimal discountAmount;
        String calcDetail;
        if (CouponTypeEnum.FIXED_DISCOUNT.getCode().equals(coupon.getCouponType())) {
            discountAmount = coupon.getDiscountValue();
            if (discountAmount.compareTo(request.getOrderAmount()) > 0) {
                discountAmount = request.getOrderAmount();
            }
            calcDetail = "固定抵扣" + coupon.getDiscountValue() + "元";
        } else if (CouponTypeEnum.PERCENT_DISCOUNT.getCode().equals(coupon.getCouponType())) {
            BigDecimal rate = coupon.getDiscountValue().divide(BigDecimal.valueOf(10), 4, RoundingMode.HALF_UP);
            discountAmount = request.getOrderAmount().multiply(BigDecimal.ONE.subtract(rate)).setScale(2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscount() != null && discountAmount.compareTo(coupon.getMaxDiscount()) > 0) {
                discountAmount = coupon.getMaxDiscount();
                calcDetail = rate.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP) + "折抵扣，上限" + coupon.getMaxDiscount() + "元";
            } else {
                calcDetail = rate.multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP) + "折抵扣";
            }
        } else {
            throw new BusinessException("不支持的优惠券类型");
        }

        if (discountAmount.compareTo(request.getOrderAmount()) > 0) {
            discountAmount = request.getOrderAmount();
        }

        CouponDiscountCalcResult result = new CouponDiscountCalcResult();
        result.setCouponCode(coupon.getCouponCode());
        result.setCouponName(coupon.getCouponName());
        result.setCouponType(coupon.getCouponType());
        CouponTypeEnum typeEnum = CouponTypeEnum.getByCode(coupon.getCouponType());
        result.setCouponTypeDesc(typeEnum != null ? typeEnum.getDesc() : "未知");
        result.setOrderAmount(request.getOrderAmount());
        result.setDiscountAmount(discountAmount);
        result.setActualAmount(request.getOrderAmount().subtract(discountAmount));
        result.setCalcDetail(calcDetail);
        return result;
    }

    private Coupon getByCouponCodeEntity(String couponCode) {
        LambdaQueryWrapper<Coupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Coupon::getCouponCode, couponCode);
        Coupon coupon = couponMapper.selectOne(wrapper);
        if (coupon == null) {
            throw new BusinessException("优惠券不存在");
        }
        return coupon;
    }

    private CouponVO toVO(Coupon coupon) {
        CouponVO vo = new CouponVO();
        vo.setId(coupon.getId());
        vo.setCouponCode(coupon.getCouponCode());
        vo.setMerchantNo(coupon.getMerchantNo());
        vo.setCouponName(coupon.getCouponName());
        vo.setCouponType(coupon.getCouponType());
        CouponTypeEnum typeEnum = CouponTypeEnum.getByCode(coupon.getCouponType());
        vo.setCouponTypeDesc(typeEnum != null ? typeEnum.getDesc() : "未知");
        vo.setDiscountValue(coupon.getDiscountValue());
        vo.setMinOrderAmount(coupon.getMinOrderAmount());
        vo.setMaxDiscount(coupon.getMaxDiscount());
        vo.setTotalQuantity(coupon.getTotalQuantity());
        vo.setIssuedCount(coupon.getIssuedCount());
        vo.setUsedCount(coupon.getUsedCount());
        vo.setStartTime(coupon.getStartTime());
        vo.setEndTime(coupon.getEndTime());
        vo.setStatus(coupon.getStatus());
        CouponStatusEnum statusEnum = CouponStatusEnum.getByCode(coupon.getStatus());
        vo.setStatusDesc(statusEnum != null ? statusEnum.getDesc() : "未知");
        vo.setRemark(coupon.getRemark());
        vo.setCreatedAt(coupon.getCreatedAt());
        vo.setUpdatedAt(coupon.getUpdatedAt());
        return vo;
    }
}
