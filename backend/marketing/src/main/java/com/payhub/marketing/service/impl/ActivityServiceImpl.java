package com.payhub.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.marketing.dto.ActivitySaveRequest;
import com.payhub.marketing.dto.ActivityVO;
import com.payhub.marketing.entity.Activity;
import com.payhub.marketing.enums.ActivityStatusEnum;
import com.payhub.marketing.enums.ActivityTypeEnum;
import com.payhub.marketing.mapper.ActivityMapper;
import com.payhub.marketing.service.ActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
public class ActivityServiceImpl implements ActivityService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ActivityMapper activityMapper;

    @Override
    public IPage<ActivityVO> listPage(Long current, Long size, String merchantNo, Map<String, Object> params) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(merchantNo), Activity::getMerchantNo, merchantNo);
        if (params != null) {
            wrapper.eq(params.get("activityCode") != null, Activity::getActivityCode, params.get("activityCode"));
            wrapper.eq(params.get("activityType") != null, Activity::getActivityType, params.get("activityType"));
            wrapper.eq(params.get("status") != null, Activity::getStatus, params.get("status"));
            wrapper.like(params.get("activityName") != null, Activity::getActivityName, params.get("activityName"));
        }
        wrapper.orderByDesc(Activity::getCreatedAt);
        IPage<Activity> page = activityMapper.selectPage(new Page<>(current, size), wrapper);
        return page.convert(this::toVO);
    }

    @Override
    public ActivityVO getByActivityCode(String activityCode) {
        Activity activity = getByActivityCodeEntity(activityCode);
        return toVO(activity);
    }

    @Override
    public void saveActivity(ActivitySaveRequest request) {
        Activity activity;
        if (request.getId() != null) {
            activity = activityMapper.selectById(request.getId());
            if (activity == null) {
                throw new BusinessException("活动不存在");
            }
            if (ActivityStatusEnum.ACTIVE.getCode().equals(activity.getStatus())) {
                throw new BusinessException("进行中的活动不可修改，请先暂停");
            }
        } else {
            activity = new Activity();
            activity.setActivityCode(OrderNoGenerator.generateWithPrefix("ACT"));
            activity.setStatus(ActivityStatusEnum.NOT_STARTED.getCode());
        }
        activity.setMerchantNo(request.getMerchantNo());
        activity.setActivityName(request.getActivityName());
        activity.setActivityType(request.getActivityType());
        activity.setThresholdAmount(request.getThresholdAmount());
        activity.setDiscountAmount(request.getDiscountAmount());
        activity.setDiscountRate(request.getDiscountRate());
        activity.setMaxDiscount(request.getMaxDiscount());
        if (StringUtils.hasText(request.getStartTime())) {
            activity.setStartTime(LocalDateTime.parse(request.getStartTime(), DTF));
        }
        if (StringUtils.hasText(request.getEndTime())) {
            activity.setEndTime(LocalDateTime.parse(request.getEndTime(), DTF));
        }
        activity.setRemark(request.getRemark());
        if (request.getId() != null) {
            activityMapper.updateById(activity);
        } else {
            activityMapper.insert(activity);
        }
    }

    @Override
    public void toggleStatus(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        if (ActivityStatusEnum.ACTIVE.getCode().equals(activity.getStatus())) {
            activity.setStatus(ActivityStatusEnum.PAUSED.getCode());
        } else if (ActivityStatusEnum.PAUSED.getCode().equals(activity.getStatus()) ||
                ActivityStatusEnum.NOT_STARTED.getCode().equals(activity.getStatus())) {
            if (activity.getEndTime() != null && activity.getEndTime().isBefore(LocalDateTime.now())) {
                activity.setStatus(ActivityStatusEnum.ENDED.getCode());
                throw new BusinessException("活动已过期，无法启用");
            }
            activity.setStatus(ActivityStatusEnum.ACTIVE.getCode());
        } else {
            throw new BusinessException("当前状态不允许切换");
        }
        activityMapper.updateById(activity);
    }

    @Override
    public void deleteActivity(Long id) {
        Activity activity = activityMapper.selectById(id);
        if (activity != null && ActivityStatusEnum.ACTIVE.getCode().equals(activity.getStatus())) {
            throw new BusinessException("进行中的活动不可删除，请先暂停");
        }
        activityMapper.deleteById(id);
    }

    private Activity getByActivityCodeEntity(String activityCode) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getActivityCode, activityCode);
        Activity activity = activityMapper.selectOne(wrapper);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }
        return activity;
    }

    private ActivityVO toVO(Activity activity) {
        ActivityVO vo = new ActivityVO();
        vo.setId(activity.getId());
        vo.setActivityCode(activity.getActivityCode());
        vo.setMerchantNo(activity.getMerchantNo());
        vo.setActivityName(activity.getActivityName());
        vo.setActivityType(activity.getActivityType());
        ActivityTypeEnum typeEnum = ActivityTypeEnum.getByCode(activity.getActivityType());
        vo.setActivityTypeDesc(typeEnum != null ? typeEnum.getDesc() : "未知");
        vo.setThresholdAmount(activity.getThresholdAmount());
        vo.setDiscountAmount(activity.getDiscountAmount());
        vo.setDiscountRate(activity.getDiscountRate());
        vo.setMaxDiscount(activity.getMaxDiscount());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setStatus(activity.getStatus());
        ActivityStatusEnum statusEnum = ActivityStatusEnum.getByCode(activity.getStatus());
        vo.setStatusDesc(statusEnum != null ? statusEnum.getDesc() : "未知");
        vo.setRemark(activity.getRemark());
        vo.setCreatedAt(activity.getCreatedAt());
        vo.setUpdatedAt(activity.getUpdatedAt());
        return vo;
    }
}
