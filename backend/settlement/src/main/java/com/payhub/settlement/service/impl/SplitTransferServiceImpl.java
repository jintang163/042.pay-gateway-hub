package com.payhub.settlement.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.channel.transfer.TransferResult;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.settlement.entity.PaySplitDetail;
import com.payhub.settlement.mapper.PaySplitDetailMapper;
import com.payhub.settlement.service.SplitTransferAlertService;
import com.payhub.settlement.service.SplitTransferService;
import com.payhub.settlement.service.UnifiedTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SplitTransferServiceImpl extends ServiceImpl<PaySplitDetailMapper, PaySplitDetail> implements SplitTransferService {

    public static final int TRANSFER_STATUS_PENDING = 0;
    public static final int TRANSFER_STATUS_PROCESSING = 1;
    public static final int TRANSFER_STATUS_SUCCESS = 2;
    public static final int TRANSFER_STATUS_FAIL = 3;

    @Value("${payhub.split.transfer.max-retry-count:5}")
    private int maxRetryCount;

    @Autowired
    private UnifiedTransferService unifiedTransferService;

    @Autowired(required = false)
    private SplitTransferAlertService splitTransferAlertService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean executeTransfer(PaySplitDetail detail) {
        if (detail == null) {
            return false;
        }
        return executeTransferById(detail.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean executeTransferById(Long splitDetailId) {
        PaySplitDetail detail = this.getById(splitDetailId);
        if (detail == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分账明细不存在");
        }
        log.info("开始执行分账代付, splitDetailNo={}, orderNo={}, receiverAccount={}",
                detail.getSplitDetailNo(), detail.getOrderNo(), detail.getReceiverAccount());

        if (TRANSFER_STATUS_SUCCESS == detail.getTransferStatus()) {
            log.warn("分账明细已代付成功，无需重复执行: splitDetailNo={}", detail.getSplitDetailNo());
            return true;
        }

        if (detail.getTransferRetryCount() != null && detail.getTransferRetryCount() >= maxRetryCount) {
            log.error("分账明细已达最大重试次数，不再执行代付: splitDetailNo={}, retryCount={}",
                    detail.getSplitDetailNo(), detail.getTransferRetryCount());
            if (splitTransferAlertService != null) {
                splitTransferAlertService.alertTransferRetryExhausted(detail);
            }
            return false;
        }

        if (detail.getSplitAmount() == null || detail.getSplitAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("分账金额无效，跳过代付并标记成功: splitDetailNo={}, amount={}",
                    detail.getSplitDetailNo(), detail.getSplitAmount());
            detail.setTransferStatus(TRANSFER_STATUS_SUCCESS);
            detail.setTransferTime(java.time.LocalDateTime.now());
            this.updateById(detail);
            return true;
        }

        UnifiedTransferService.TransferContext ctx;
        try {
            ctx = unifiedTransferService.buildContextForSplitDetail(detail.getId());
        } catch (BusinessException e) {
            log.error("构建代付上下文失败: splitDetailNo={}", detail.getSplitDetailNo(), e);
            detail.setTransferStatus(TRANSFER_STATUS_FAIL);
            detail.setTransferFailReason(e.getMessage());
            this.updateById(detail);
            return false;
        }

        TransferResult result;
        try {
            result = unifiedTransferService.executeTransfer(ctx);
        } catch (BusinessException e) {
            log.error("分账代付业务异常: splitDetailNo={}", detail.getSplitDetailNo(), e);
            detail = this.getById(detail.getId());
            detail.setTransferStatus(TRANSFER_STATUS_FAIL);
            if (StrUtil.isBlank(detail.getTransferFailReason())) {
                detail.setTransferFailReason(e.getMessage());
            }
            this.updateById(detail);
            triggerAlert(detail);
            return false;
        } catch (Exception e) {
            log.error("分账代付系统异常: splitDetailNo={}", detail.getSplitDetailNo(), e);
            detail = this.getById(detail.getId());
            detail.setTransferStatus(TRANSFER_STATUS_FAIL);
            if (StrUtil.isBlank(detail.getTransferFailReason())) {
                detail.setTransferFailReason("系统异常: " + e.getMessage());
            }
            this.updateById(detail);
            triggerAlert(detail);
            return false;
        }

        triggerAlert(this.getById(detail.getId()));
        return result.isSuccess();
    }

    private void triggerAlert(PaySplitDetail detail) {
        if (detail == null || splitTransferAlertService == null) {
            return;
        }
        try {
            if (TRANSFER_STATUS_FAIL == detail.getTransferStatus()) {
                if (detail.getTransferRetryCount() != null && detail.getTransferRetryCount() >= maxRetryCount) {
                    splitTransferAlertService.alertTransferRetryExhausted(detail);
                } else {
                    splitTransferAlertService.alertTransferFailed(detail);
                }
            }
        } catch (Exception e) {
            log.warn("发送分账代付告警异常: splitDetailNo={}", detail.getSplitDetailNo(), e);
        }
    }

    @Override
    public boolean executeTransferBatch(List<PaySplitDetail> details) {
        if (details == null || details.isEmpty()) {
            return true;
        }
        boolean allSuccess = true;
        for (PaySplitDetail detail : details) {
            try {
                boolean success = executeTransferById(detail.getId());
                if (!success) {
                    allSuccess = false;
                }
            } catch (Exception e) {
                log.error("批量分账代付单条执行异常: splitDetailNo={}", detail.getSplitDetailNo(), e);
                allSuccess = false;
            }
        }
        return allSuccess;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean retryTransfer(Long splitDetailId) {
        PaySplitDetail detail = this.getById(splitDetailId);
        if (detail == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分账明细不存在");
        }
        return executeTransferById(splitDetailId);
    }

    @Override
    public IPage<PaySplitDetail> listPendingTransfers(Long current, Long size, Map<String, Object> params) {
        Page<PaySplitDetail> page = new Page<>(current, size);
        LambdaQueryWrapper<PaySplitDetail> wrapper = new LambdaQueryWrapper<>();
        if (params != null) {
            if (params.get("merchantNo") != null) {
                wrapper.eq(PaySplitDetail::getMerchantNo, params.get("merchantNo"));
            }
            if (params.get("orderNo") != null) {
                wrapper.eq(PaySplitDetail::getOrderNo, params.get("orderNo"));
            }
            if (params.get("transferStatus") != null) {
                wrapper.eq(PaySplitDetail::getTransferStatus, params.get("transferStatus"));
            }
        }
        wrapper.and(w -> w.eq(PaySplitDetail::getTransferStatus, TRANSFER_STATUS_PENDING)
                .or().eq(PaySplitDetail::getTransferStatus, TRANSFER_STATUS_FAIL)
                .or().eq(PaySplitDetail::getTransferStatus, TRANSFER_STATUS_PROCESSING)
                .or().isNull(PaySplitDetail::getTransferStatus));
        wrapper.orderByDesc(PaySplitDetail::getId);
        return this.page(page, wrapper);
    }

    @Override
    public List<PaySplitDetail> loadPendingTransferList(int limit) {
        return this.baseMapper.selectPendingTransferList(limit);
    }

    @Override
    public boolean processPendingTransfers(int limit) {
        List<PaySplitDetail> pendingList = loadPendingTransferList(limit);
        if (pendingList == null || pendingList.isEmpty()) {
            log.info("无待处理的分账代付任务");
            return true;
        }
        log.info("开始处理待分账代付任务, 数量: {}", pendingList.size());
        return executeTransferBatch(pendingList);
    }
}
