package com.payhub.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.settlement.dto.ReportSubscriptionSaveRequest;
import com.payhub.settlement.dto.ReportSubscriptionVO;
import com.payhub.settlement.entity.ReportSubscription;
import com.payhub.settlement.mapper.ReportSubscriptionMapper;
import com.payhub.settlement.service.ReportSubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
public class ReportSubscriptionServiceImpl extends ServiceImpl<ReportSubscriptionMapper, ReportSubscription> implements ReportSubscriptionService {

    @Override
    public IPage<ReportSubscriptionVO> listPage(Long current, Long size, Map<String, Object> params) {
        Page<ReportSubscription> page = new Page<>(current, size);
        LambdaQueryWrapper<ReportSubscription> wrapper = new LambdaQueryWrapper<>();
        if (params != null) {
            if (params.get("merchantNo") != null) {
                wrapper.eq(ReportSubscription::getMerchantNo, params.get("merchantNo"));
            }
            if (params.get("reportType") != null) {
                wrapper.eq(ReportSubscription::getReportType, params.get("reportType"));
            }
            if (params.get("reportCategory") != null) {
                wrapper.like(ReportSubscription::getReportCategory, params.get("reportCategory"));
            }
            if (params.get("enabled") != null) {
                wrapper.eq(ReportSubscription::getEnabled, params.get("enabled"));
            }
        }
        wrapper.orderByDesc(ReportSubscription::getId);
        IPage<ReportSubscription> subscriptionPage = this.page(page, wrapper);
        return subscriptionPage.convert(this::convertToVO);
    }

    @Override
    public ReportSubscriptionVO getSubscriptionById(Long id) {
        ReportSubscription subscription = this.getById(id);
        if (subscription == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报表订阅不存在");
        }
        return convertToVO(subscription);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSubscription(ReportSubscriptionSaveRequest request) {
        ReportSubscription subscription = BeanUtil.copyProperties(request, ReportSubscription.class);
        subscription.setSubscriptionNo(OrderNoGenerator.generateWithPrefix("RS"));
        if (subscription.getEnabled() == null) {
            subscription.setEnabled(1);
        }
        this.save(subscription);
        log.info("报表订阅保存成功: id={}, subscriptionNo={}, merchantNo={}", subscription.getId(), subscription.getSubscriptionNo(), request.getMerchantNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSubscription(Long id, ReportSubscriptionSaveRequest request) {
        ReportSubscription subscription = this.getById(id);
        if (subscription == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报表订阅不存在");
        }
        subscription.setReportType(request.getReportType());
        subscription.setReportCategory(request.getReportCategory());
        subscription.setPushChannel(request.getPushChannel());
        subscription.setEmailList(request.getEmailList());
        subscription.setPhoneList(request.getPhoneList());
        subscription.setPushTime(request.getPushTime());
        if (request.getEnabled() != null) {
            subscription.setEnabled(request.getEnabled());
        }
        subscription.setRemark(request.getRemark());
        this.updateById(subscription);
        log.info("报表订阅更新成功: id={}, merchantNo={}", id, request.getMerchantNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubscription(Long id) {
        ReportSubscription subscription = this.getById(id);
        if (subscription == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报表订阅不存在");
        }
        this.removeById(id);
        log.info("报表订阅删除成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleSubscription(Long id) {
        ReportSubscription subscription = this.getById(id);
        if (subscription == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报表订阅不存在");
        }
        subscription.setEnabled(subscription.getEnabled() == 1 ? 0 : 1);
        this.updateById(subscription);
        log.info("报表订阅状态切换成功: id={}, status={}", id, subscription.getEnabled());
    }

    @Override
    public boolean validateOwnership(Long id, String merchantNo) {
        ReportSubscription subscription = this.getById(id);
        if (subscription == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报表订阅不存在");
        }
        return StrUtil.equals(subscription.getMerchantNo(), merchantNo);
    }

    private ReportSubscriptionVO convertToVO(ReportSubscription subscription) {
        ReportSubscriptionVO vo = BeanUtil.copyProperties(subscription, ReportSubscriptionVO.class);
        vo.setEnabledDesc(subscription.getEnabled() == 1 ? "启用" : "禁用");
        vo.setReportTypeDesc(getReportTypeDesc(subscription.getReportType()));
        vo.setPushChannelDesc(getPushChannelDesc(subscription.getPushChannel()));
        return vo;
    }

    private String getReportTypeDesc(Integer reportType) {
        if (reportType == null) return "";
        switch (reportType) {
            case 1: return "日报";
            case 2: return "周报";
            case 3: return "月报";
            default: return "未知";
        }
    }

    private String getPushChannelDesc(Integer pushChannel) {
        if (pushChannel == null) return "";
        switch (pushChannel) {
            case 1: return "邮件";
            case 2: return "短信";
            case 3: return "邮件+短信";
            default: return "未知";
        }
    }
}
