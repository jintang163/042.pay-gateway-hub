package com.payhub.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.settlement.dto.ReportPushRecordVO;
import com.payhub.settlement.entity.ReportPushRecord;
import com.payhub.settlement.mapper.ReportPushRecordMapper;
import com.payhub.settlement.service.ReportPushRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Service
public class ReportPushRecordServiceImpl extends ServiceImpl<ReportPushRecordMapper, ReportPushRecord> implements ReportPushRecordService {

    @Override
    public IPage<ReportPushRecordVO> listPage(Long current, Long size, Map<String, Object> params) {
        Page<ReportPushRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<ReportPushRecord> wrapper = new LambdaQueryWrapper<>();
        if (params != null) {
            if (params.get("merchantNo") != null) {
                wrapper.eq(ReportPushRecord::getMerchantNo, params.get("merchantNo"));
            }
            if (params.get("subscriptionNo") != null) {
                wrapper.like(ReportPushRecord::getSubscriptionNo, params.get("subscriptionNo"));
            }
            if (params.get("reportType") != null) {
                wrapper.eq(ReportPushRecord::getReportType, params.get("reportType"));
            }
            if (params.get("pushStatus") != null) {
                wrapper.eq(ReportPushRecord::getPushStatus, params.get("pushStatus"));
            }
            if (params.get("startDate") != null) {
                wrapper.ge(ReportPushRecord::getStartDate, params.get("startDate"));
            }
            if (params.get("endDate") != null) {
                wrapper.le(ReportPushRecord::getEndDate, params.get("endDate"));
            }
        }
        wrapper.orderByDesc(ReportPushRecord::getId);
        IPage<ReportPushRecord> recordPage = this.page(page, wrapper);
        return recordPage.convert(this::convertToVO);
    }

    @Override
    public ReportPushRecordVO getRecordById(Long id) {
        ReportPushRecord record = this.getById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报表推送记录不存在");
        }
        return convertToVO(record);
    }

    @Override
    public boolean validateOwnership(Long id, String merchantNo) {
        ReportPushRecord record = this.getById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报表推送记录不存在");
        }
        return StrUtil.equals(record.getMerchantNo(), merchantNo);
    }

    private ReportPushRecordVO convertToVO(ReportPushRecord record) {
        ReportPushRecordVO vo = BeanUtil.copyProperties(record, ReportPushRecordVO.class);
        vo.setReportTypeDesc(getReportTypeDesc(record.getReportType()));
        vo.setPushStatusDesc(getPushStatusDesc(record.getPushStatus()));
        vo.setPushChannelDesc(getPushChannelDesc(record.getPushChannel()));
        vo.setTriggerTypeDesc(getTriggerTypeDesc(record.getTriggerType()));
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

    private String getPushStatusDesc(Integer pushStatus) {
        if (pushStatus == null) return "";
        switch (pushStatus) {
            case 0: return "待推送";
            case 1: return "推送成功";
            case 2: return "推送失败";
            case 3: return "推送中";
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

    private String getTriggerTypeDesc(Integer triggerType) {
        if (triggerType == null) return "";
        switch (triggerType) {
            case 1: return "定时推送";
            case 2: return "手动推送";
            default: return "未知";
        }
    }
}
