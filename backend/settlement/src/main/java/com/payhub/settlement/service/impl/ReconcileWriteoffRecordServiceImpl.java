package com.payhub.settlement.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.enums.PayChannelEnum;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.settlement.dto.WriteoffRecordVO;
import com.payhub.settlement.entity.ReconcileWriteoffRecord;
import com.payhub.settlement.enums.*;
import com.payhub.settlement.mapper.ReconcileWriteoffRecordMapper;
import com.payhub.settlement.service.ReconcileWriteoffRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class ReconcileWriteoffRecordServiceImpl extends ServiceImpl<ReconcileWriteoffRecordMapper, ReconcileWriteoffRecord>
        implements ReconcileWriteoffRecordService {

    @Override
    public IPage<WriteoffRecordVO> listPage(Long current, Long size, Map<String, Object> params) {
        LambdaQueryWrapper<ReconcileWriteoffRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconcileWriteoffRecord::getDeleted, 0);

        if (params != null) {
            if (params.get("reconcileNo") != null && StrUtil.isNotBlank(params.get("reconcileNo").toString())) {
                wrapper.eq(ReconcileWriteoffRecord::getReconcileNo, params.get("reconcileNo").toString());
            }
            if (params.get("merchantNo") != null && StrUtil.isNotBlank(params.get("merchantNo").toString())) {
                wrapper.eq(ReconcileWriteoffRecord::getMerchantNo, params.get("merchantNo").toString());
            }
            if (params.get("payChannel") != null && StrUtil.isNotBlank(params.get("payChannel").toString())) {
                wrapper.eq(ReconcileWriteoffRecord::getPayChannel, params.get("payChannel").toString());
            }
            if (params.get("writeoffStatus") != null) {
                wrapper.eq(ReconcileWriteoffRecord::getWriteoffStatus, Integer.parseInt(params.get("writeoffStatus").toString()));
            }
            if (params.get("writeoffSource") != null) {
                wrapper.eq(ReconcileWriteoffRecord::getWriteoffSource, Integer.parseInt(params.get("writeoffSource").toString()));
            }
            if (params.get("writeoffType") != null) {
                wrapper.eq(ReconcileWriteoffRecord::getWriteoffType, Integer.parseInt(params.get("writeoffType").toString()));
            }
        }

        wrapper.orderByDesc(ReconcileWriteoffRecord::getCreatedAt);

        IPage<ReconcileWriteoffRecord> page = this.page(new Page<>(current, size), wrapper);
        return page.convert(this::convertToVO);
    }

    @Override
    public WriteoffRecordVO getByWriteoffNo(String writeoffNo) {
        if (StrUtil.isBlank(writeoffNo)) {
            return null;
        }
        LambdaQueryWrapper<ReconcileWriteoffRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconcileWriteoffRecord::getWriteoffNo, writeoffNo);
        ReconcileWriteoffRecord record = this.getOne(wrapper);
        return record != null ? convertToVO(record) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeWriteoff(Long id) {
        ReconcileWriteoffRecord record = this.getById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "补账记录不存在");
        }

        if (!WriteoffStatusEnum.PENDING.getCode().equals(record.getWriteoffStatus())
                && !WriteoffStatusEnum.FAIL.getCode().equals(record.getWriteoffStatus())) {
            throw new BusinessException(ResultCode.FAIL, "当前状态不允许执行平账");
        }

        record.setWriteoffStatus(WriteoffStatusEnum.EXECUTING.getCode());
        this.updateById(record);

        try {
            doExecuteWriteoff(record);
            record.setWriteoffStatus(WriteoffStatusEnum.SUCCESS.getCode());
            record.setExecuteTime(LocalDateTime.now());
            record.setExecuteResult("平账执行成功");
            this.updateById(record);
            log.info("自动平账执行成功，writeoffNo：{}", record.getWriteoffNo());
        } catch (Exception e) {
            log.error("自动平账执行失败，writeoffNo：{}，error：{}", record.getWriteoffNo(), e.getMessage());
            record.setWriteoffStatus(WriteoffStatusEnum.FAIL.getCode());
            record.setExecuteResult("平账执行失败：" + e.getMessage());
            this.updateById(record);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryWriteoff(Long id) {
        ReconcileWriteoffRecord record = this.getById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "补账记录不存在");
        }
        if (!WriteoffStatusEnum.FAIL.getCode().equals(record.getWriteoffStatus())) {
            throw new BusinessException(ResultCode.FAIL, "只有失败的记录才能重试");
        }

        record.setWriteoffStatus(WriteoffStatusEnum.PENDING.getCode());
        record.setExecuteResult(null);
        this.updateById(record);

        executeWriteoff(id);
    }

    private void doExecuteWriteoff(ReconcileWriteoffRecord record) {
        WriteoffTypeEnum typeEnum = WriteoffTypeEnum.getByCode(record.getWriteoffType());
        if (typeEnum == null) {
            throw new BusinessException(ResultCode.FAIL, "未知的平账类型");
        }

        switch (typeEnum) {
            case IGNORE:
                log.info("忽略类型平账，直接标记成功，writeoffNo：{}", record.getWriteoffNo());
                break;
            case SUPPLEMENT:
            case REFUND:
            case ADJUST:
                log.info("执行{}平账，writeoffNo：{}，orderNo：{}", typeEnum.getDesc(), record.getWriteoffNo(), record.getOrderNo());
                break;
            default:
                throw new BusinessException(ResultCode.FAIL, "不支持的平账类型");
        }
    }

    private WriteoffRecordVO convertToVO(ReconcileWriteoffRecord entity) {
        WriteoffRecordVO vo = new WriteoffRecordVO();
        vo.setId(entity.getId());
        vo.setWriteoffNo(entity.getWriteoffNo());
        vo.setReconcileNo(entity.getReconcileNo());
        vo.setDetailId(entity.getDetailId());
        vo.setDetailNo(entity.getDetailNo());
        vo.setMerchantNo(entity.getMerchantNo());
        vo.setPayChannel(entity.getPayChannel());

        if (StrUtil.isNotBlank(entity.getPayChannel())) {
            PayChannelEnum channelEnum = PayChannelEnum.getByCode(entity.getPayChannel());
            vo.setPayChannelDesc(channelEnum != null ? channelEnum.getDesc() : entity.getPayChannel());
        }

        vo.setDiffType(entity.getDiffType());
        if (entity.getDiffType() != null) {
            ReconcileDiffTypeEnum diffTypeEnum = ReconcileDiffTypeEnum.getByCode(entity.getDiffType());
            vo.setDiffTypeDesc(diffTypeEnum != null ? diffTypeEnum.getDesc() : "");
        }

        vo.setDiffAmount(entity.getDiffAmount());
        vo.setWriteoffAmount(entity.getWriteoffAmount());
        vo.setWriteoffType(entity.getWriteoffType());
        if (entity.getWriteoffType() != null) {
            WriteoffTypeEnum typeEnum = WriteoffTypeEnum.getByCode(entity.getWriteoffType());
            vo.setWriteoffTypeDesc(typeEnum != null ? typeEnum.getDesc() : "");
        }

        vo.setWriteoffSource(entity.getWriteoffSource());
        if (entity.getWriteoffSource() != null) {
            WriteoffSourceEnum sourceEnum = WriteoffSourceEnum.getByCode(entity.getWriteoffSource());
            vo.setWriteoffSourceDesc(sourceEnum != null ? sourceEnum.getDesc() : "");
        }

        vo.setRuleId(entity.getRuleId());
        vo.setRuleName(entity.getRuleName());
        vo.setWriteoffStatus(entity.getWriteoffStatus());
        if (entity.getWriteoffStatus() != null) {
            WriteoffStatusEnum statusEnum = WriteoffStatusEnum.getByCode(entity.getWriteoffStatus());
            vo.setWriteoffStatusDesc(statusEnum != null ? statusEnum.getDesc() : "");
        }

        vo.setErrorOrderNo(entity.getErrorOrderNo());
        vo.setOrderNo(entity.getOrderNo());
        vo.setChannelTradeNo(entity.getChannelTradeNo());
        vo.setWriteoffRemark(entity.getWriteoffRemark());
        vo.setExecuteTime(entity.getExecuteTime());
        vo.setExecuteResult(entity.getExecuteResult());
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
