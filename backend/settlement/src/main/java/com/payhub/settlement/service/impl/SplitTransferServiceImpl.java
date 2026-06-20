package com.payhub.settlement.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.channel.transfer.TransferRequest;
import com.payhub.channel.transfer.TransferResult;
import com.payhub.channel.transfer.TransferService;
import com.payhub.channel.transfer.TransferServiceFactory;
import com.payhub.common.context.SandboxContext;
import com.payhub.common.enums.SandboxSceneEnum;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.settlement.entity.PaySplitDetail;
import com.payhub.settlement.entity.SplitReceiver;
import com.payhub.settlement.enums.SplitReceiverVerifyStatusEnum;
import com.payhub.settlement.mapper.PaySplitDetailMapper;
import com.payhub.settlement.service.SplitReceiverService;
import com.payhub.settlement.service.SplitTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SplitTransferServiceImpl extends ServiceImpl<PaySplitDetailMapper, PaySplitDetail> implements SplitTransferService {

    public static final int TRANSFER_STATUS_PENDING = 0;
    public static final int TRANSFER_STATUS_PROCESSING = 1;
    public static final int TRANSFER_STATUS_SUCCESS = 2;
    public static final int TRANSFER_STATUS_FAIL = 3;

    public static final int DEFAULT_MAX_RETRY_COUNT = 5;

    @Value("${payhub.split.transfer.default-channel:UNION_PAY}")
    private String defaultTransferChannel;

    @Value("${payhub.split.transfer.max-retry-count:5}")
    private int maxRetryCount;

    @Value("${payhub.split.transfer.retry-delay-minutes:30}")
    private int retryDelayMinutes;

    @Autowired(required = false)
    private TransferServiceFactory transferServiceFactory;

    @Autowired
    private SplitReceiverService splitReceiverService;

    @Autowired(required = false)
    private SplitTransferAlertService splitTransferAlertService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean executeTransfer(PaySplitDetail detail) {
        if (detail == null) {
            return false;
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
            return false;
        }

        String merchantNo = detail.getMerchantNo();
        String receiverAccount = detail.getReceiverAccount();
        SplitReceiver receiver = splitReceiverService.checkReceiverVerified(receiverAccount, merchantNo);
        if (receiver == null) {
            log.error("分账接收方不存在或未认证: splitDetailNo={}, receiverAccount={}",
                    detail.getSplitDetailNo(), receiverAccount);
            detail.setTransferStatus(TRANSFER_STATUS_FAIL);
            detail.setTransferFailReason("分账接收方不存在或未认证");
            this.updateById(detail);
            return false;
        }

        if (detail.getSplitAmount() == null || detail.getSplitAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("分账金额无效，跳过代付: splitDetailNo={}, amount={}", detail.getSplitDetailNo(), detail.getSplitAmount());
            detail.setTransferStatus(TRANSFER_STATUS_SUCCESS);
            detail.setTransferTime(LocalDateTime.now());
            this.updateById(detail);
            return true;
        }

        String transferNo = detail.getTransferNo();
        if (StrUtil.isBlank(transferNo)) {
            transferNo = OrderNoGenerator.generateWithPrefix("TF");
            detail.setTransferNo(transferNo);
        }

        String channel = StrUtil.isNotBlank(detail.getTransferChannel()) ? detail.getTransferChannel() : defaultTransferChannel;
        detail.setTransferChannel(channel);

        detail.setTransferStatus(TRANSFER_STATUS_PROCESSING);
        int currentRetryCount = detail.getTransferRetryCount() == null ? 0 : detail.getTransferRetryCount();
        detail.setTransferRetryCount(currentRetryCount + 1);
        this.updateById(detail);

        TransferResult result;
        try {
            TransferRequest request = buildTransferRequest(detail, receiver, transferNo, channel);
            result = callTransferService(channel, request);
        } catch (Exception e) {
            log.error("调用代付通道异常: splitDetailNo={}, transferNo={}, channel={}",
                    detail.getSplitDetailNo(), transferNo, channel, e);
            result = TransferResult.fail(transferNo, "E999", "代付通道调用异常: " + e.getMessage());
        }

        applyTransferResult(detail, result);
        this.updateById(detail);

        if (splitTransferAlertService != null) {
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

        log.info("分账代付执行完成: splitDetailNo={}, transferNo={}, success={}, status={}",
                detail.getSplitDetailNo(), transferNo, result.isSuccess(), result.getStatus());
        return result.isSuccess();
    }

    private TransferRequest buildTransferRequest(PaySplitDetail detail, SplitReceiver receiver, String transferNo, String channel) {
        TransferRequest request = new TransferRequest();
        request.setTransferNo(transferNo);
        request.setChannel(channel);
        request.setReceiverAccount(receiver.getBankCardNo());
        request.setReceiverName(receiver.getIdCardName());
        request.setAmount(detail.getSplitAmount());
        request.setBankName(receiver.getBankName());
        request.setBankBranchName(receiver.getBankBranchName());
        request.setReceiverType(receiver.getReceiverType());
        request.setIdCardNo(receiver.getIdCardNo());
        request.setIdCardName(receiver.getIdCardName());
        request.setBankPhone(receiver.getBankPhone());
        request.setMerchantNo(detail.getMerchantNo());
        request.setSourceType("SPLIT");
        request.setSourceNo(detail.getSplitDetailNo());
        request.setRemark("分账代付-" + detail.getOrderNo());
        return request;
    }

    private TransferResult callTransferService(String channel, TransferRequest request) {
        if (transferServiceFactory == null) {
            String scene = SandboxContext.getScene();
            if (SandboxSceneEnum.TRANSFER_FAIL.getCode().equalsIgnoreCase(scene)) {
                return TransferResult.fail(request.getTransferNo(), "E001", "沙箱强制代付失败");
            }
            if (SandboxSceneEnum.TRANSFER_EXCEPTION.getCode().equalsIgnoreCase(scene)) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "沙箱强制代付异常");
            }
            return TransferResult.success(request.getTransferNo(), "SB" + request.getTransferNo(), LocalDateTime.now());
        }

        TransferService transferService = transferServiceFactory.getTransferService(channel);
        if (transferService == null) {
            return TransferResult.fail(request.getTransferNo(), "E002", "不支持的代付通道: " + channel);
        }
        return transferService.transfer(request);
    }

    private void applyTransferResult(PaySplitDetail detail, TransferResult result) {
        if (result == null) {
            detail.setTransferStatus(TRANSFER_STATUS_FAIL);
            detail.setTransferFailReason("代付结果为空");
            scheduleNextRetry(detail);
            return;
        }

        detail.setChannelTransferNo(result.getChannelTransferNo());

        if (result.isSuccess()) {
            detail.setTransferStatus(TRANSFER_STATUS_SUCCESS);
            detail.setTransferTime(result.getCompleteTime() != null ? result.getCompleteTime() : LocalDateTime.now());
            detail.setTransferFailReason(null);
            detail.setNextTransferRetryTime(null);
        } else if (TransferResult.STATUS_PROCESSING.equals(result.getStatus())) {
            detail.setTransferStatus(TRANSFER_STATUS_PROCESSING);
            detail.setNextTransferRetryTime(LocalDateTime.now().plusMinutes(retryDelayMinutes));
        } else {
            detail.setTransferStatus(TRANSFER_STATUS_FAIL);
            detail.setTransferFailReason(result.getFailReason());
            scheduleNextRetry(detail);
        }
    }

    private void scheduleNextRetry(PaySplitDetail detail) {
        if (detail.getTransferRetryCount() == null || detail.getTransferRetryCount() < maxRetryCount) {
            long delayMinutes = (long) retryDelayMinutes * detail.getTransferRetryCount();
            detail.setNextTransferRetryTime(LocalDateTime.now().plusMinutes(Math.max(delayMinutes, retryDelayMinutes)));
        } else {
            detail.setNextTransferRetryTime(null);
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
                boolean success = executeTransfer(detail);
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
        return executeTransfer(detail);
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
                .or()
                .eq(PaySplitDetail::getTransferStatus, TRANSFER_STATUS_FAIL)
                .or()
                .eq(PaySplitDetail::getTransferStatus, TRANSFER_STATUS_PROCESSING));
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
