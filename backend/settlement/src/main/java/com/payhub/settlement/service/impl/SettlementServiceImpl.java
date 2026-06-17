package com.payhub.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.channel.transfer.TransferRequest;
import com.payhub.channel.transfer.TransferResult;
import com.payhub.channel.transfer.TransferService;
import com.payhub.channel.transfer.TransferServiceFactory;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.merchant.entity.MerchantInfo;
import com.payhub.merchant.service.MerchantInfoService;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.mapper.PayOrderMapper;
import com.payhub.settlement.dto.SettlementVO;
import com.payhub.settlement.entity.PaySplitDetail;
import com.payhub.settlement.entity.SettlementRecord;
import com.payhub.settlement.mapper.PaySplitDetailMapper;
import com.payhub.settlement.mapper.SettlementRecordMapper;
import com.payhub.settlement.service.SettlementService;
import com.payhub.settlement.service.SplitEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SettlementServiceImpl extends ServiceImpl<SettlementRecordMapper, SettlementRecord> implements SettlementService {

    @Autowired
    private PayOrderMapper payOrderMapper;

    @Autowired
    private SplitEngineService splitEngineService;

    @Autowired
    private MerchantInfoService merchantInfoService;

    @Autowired
    private PaySplitDetailMapper paySplitDetailMapper;

    @Autowired
    private TransferServiceFactory transferServiceFactory;

    private static final int MAX_RETRY_COUNT = 5;

    private static final int TRANSFER_STATUS_PENDING = 0;
    private static final int TRANSFER_STATUS_PROCESSING = 1;
    private static final int TRANSFER_STATUS_SUCCESS = 2;
    private static final int TRANSFER_STATUS_FAIL = 3;

    @Override
    public SettlementVO getBySettlementNo(String settlementNo) {
        LambdaQueryWrapper<SettlementRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SettlementRecord::getSettlementNo, settlementNo);
        SettlementRecord record = this.getOne(wrapper);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "结算记录不存在");
        }
        return convertToVO(record);
    }

    @Override
    public IPage<SettlementVO> listPage(Long current, Long size, Map<String, Object> params) {
        Page<SettlementRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<SettlementRecord> wrapper = new LambdaQueryWrapper<>();
        if (params != null) {
            if (params.get("merchantNo") != null) {
                wrapper.eq(SettlementRecord::getMerchantNo, params.get("merchantNo"));
            }
            if (params.get("settleDate") != null) {
                wrapper.eq(SettlementRecord::getSettleDate, params.get("settleDate"));
            }
            if (params.get("settleStatus") != null) {
                wrapper.eq(SettlementRecord::getSettleStatus, params.get("settleStatus"));
            }
            if (params.get("payChannel") != null) {
                wrapper.eq(SettlementRecord::getPayChannel, params.get("payChannel"));
            }
        }
        wrapper.orderByDesc(SettlementRecord::getSettleDate, SettlementRecord::getId);
        IPage<SettlementRecord> recordPage = this.page(page, wrapper);
        return recordPage.convert(this::convertToVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateSettlement(LocalDate settleDate) {
        log.info("开始生成结算记录, 结算日期: {}", settleDate);

        LocalDateTime startTime = LocalDateTime.of(settleDate, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(settleDate, LocalTime.MAX);

        LambdaQueryWrapper<PayOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(PayOrder::getPayStatus, 1)
                .between(PayOrder::getPayTime, startTime, endTime);
        List<PayOrder> orders = payOrderMapper.selectList(orderWrapper);

        if (orders.isEmpty()) {
            log.info("结算日期 {} 无成功支付订单", settleDate);
            return;
        }

        Map<String, List<PayOrder>> ordersByMerchantAndChannel = orders.stream()
                .collect(Collectors.groupingBy(order -> order.getMerchantNo() + "_" + order.getPayChannel()));

        for (Map.Entry<String, List<PayOrder>> entry : ordersByMerchantAndChannel.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split("_");
            String merchantNo = parts[0];
            String payChannel = parts[1];
            List<PayOrder> merchantOrders = entry.getValue();

            LambdaQueryWrapper<SettlementRecord> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(SettlementRecord::getMerchantNo, merchantNo)
                    .eq(SettlementRecord::getPayChannel, payChannel)
                    .eq(SettlementRecord::getSettleDate, settleDate);
            SettlementRecord existRecord = this.getOne(existWrapper);
            if (existRecord != null) {
                log.warn("商户 {} 渠道 {} 结算日期 {} 的结算记录已存在, 跳过", merchantNo, payChannel, settleDate);
                continue;
            }

            BigDecimal totalAmount = merchantOrders.stream()
                    .map(PayOrder::getPayAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal feeAmount = merchantOrders.stream()
                    .map(order -> order.getFeeAmount() != null ? order.getFeeAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal actualSettleAmount = totalAmount.subtract(feeAmount);

            MerchantInfo merchantInfo = getMerchantInfo(merchantNo);

            SettlementRecord record = new SettlementRecord();
            record.setSettlementNo(OrderNoGenerator.generateWithPrefix("ST"));
            record.setMerchantNo(merchantNo);
            record.setPayChannel(payChannel);
            record.setSettleDate(settleDate);
            record.setTotalAmount(totalAmount);
            record.setFeeAmount(feeAmount);
            record.setActualSettleAmount(actualSettleAmount);
            record.setOrderCount(merchantOrders.size());
            record.setSettleStatus(0);
            record.setRetryCount(0);

            if (merchantInfo != null) {
                record.setBankName(merchantInfo.getSettlementBankName());
                record.setBankAccount(merchantInfo.getSettlementBankAccount());
                record.setAccountName(merchantInfo.getSettlementAccountName());
            }

            this.save(record);
            log.info("生成结算记录成功: settlementNo={}, merchantNo={}, payChannel={}, totalAmount={}, actualSettleAmount={}",
                    record.getSettlementNo(), merchantNo, payChannel, totalAmount, actualSettleAmount);

            List<PaySplitDetail> splitDetails = splitEngineService.calculateBatchSplit(record.getId(), record.getSettlementNo(), merchantOrders);
            splitEngineService.saveBatch(splitDetails);
            log.info("生成分账明细成功: settlementId={}, count={}", record.getId(), splitDetails.size());
        }

        log.info("结算记录生成完成, 结算日期: {}", settleDate);
    }

    private MerchantInfo getMerchantInfo(String merchantNo) {
        try {
            LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MerchantInfo::getMerchantNo, merchantNo);
            return merchantInfoService.getOne(wrapper);
        } catch (Exception e) {
            log.warn("获取商户信息失败, merchantNo={}", merchantNo, e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmSettlement(Long id) {
        SettlementRecord record = this.getById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "结算记录不存在");
        }
        if (record.getSettleStatus() == 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该结算记录已确认");
        }
        record.setSettleStatus(1);
        this.updateById(record);
        log.info("结算确认成功: id={}, settlementNo={}", id, record.getSettlementNo());
    }

    @Override
    public void executeSettlementTask() {
        log.info("开始执行批量分账明细打款任务");

        List<PaySplitDetail> pendingDetails = paySplitDetailMapper.selectPendingTransferList(100);
        if (pendingDetails.isEmpty()) {
            log.info("没有需要处理的分账打款明细");
            return;
        }

        log.info("待处理分账打款明细数量: {}", pendingDetails.size());

        for (PaySplitDetail detail : pendingDetails) {
            try {
                processSplitDetailTransfer(detail);
            } catch (Exception e) {
                log.error("处理分账明细打款异常, id={}, splitDetailNo={}", detail.getId(), detail.getSplitDetailNo(), e);
            }
        }

        log.info("批量分账明细打款任务执行完成");
    }

    @Transactional(rollbackFor = Exception.class)
    public void processSettlement(SettlementRecord record) {
        log.info("开始处理结算记录: id={}, settlementNo={}", record.getId(), record.getSettlementNo());

        LambdaQueryWrapper<PaySplitDetail> detailWrapper = new LambdaQueryWrapper<>();
        detailWrapper.eq(PaySplitDetail::getSettlementId, record.getId());
        List<PaySplitDetail> details = paySplitDetailMapper.selectList(detailWrapper);

        if (details.isEmpty()) {
            log.warn("结算记录 {} 没有分账明细", record.getSettlementNo());
            return;
        }

        record.setSettleStatus(1);
        this.updateById(record);

        int pendingCount = 0;
        for (PaySplitDetail detail : details) {
            if (detail.getTransferStatus() == null
                    || detail.getTransferStatus() == TRANSFER_STATUS_PENDING
                    || detail.getTransferStatus() == TRANSFER_STATUS_FAIL) {
                pendingCount++;
                try {
                    processSplitDetailTransfer(detail);
                } catch (Exception e) {
                    log.error("处理分账明细打款异常, detailId={}", detail.getId(), e);
                }
            }
        }

        log.info("结算 {} 处理完成, 共{}条分账明细, 本次触发{}条待打款",
                record.getSettlementNo(), details.size(), pendingCount);
    }

    @Transactional(rollbackFor = Exception.class)
    public void processSplitDetailTransfer(PaySplitDetail detail) {
        log.info("开始处理分账明细打款: id={}, splitDetailNo={}, settlementId={}, amount={}",
                detail.getId(), detail.getSplitDetailNo(), detail.getSettlementId(), detail.getSplitAmount());

        String transferNo = OrderNoGenerator.generateWithPrefix("TF");
        detail.setTransferNo(transferNo);
        detail.setTransferStatus(TRANSFER_STATUS_PROCESSING);

        SettlementRecord settlement = this.getById(detail.getSettlementId());
        String transferChannel = determineTransferChannel(settlement, detail);
        detail.setTransferChannel(transferChannel);

        paySplitDetailMapper.updateById(detail);

        TransferRequest request = buildTransferRequest(detail, settlement, transferChannel);

        TransferResult result;
        try {
            TransferService transferService = transferServiceFactory.getTransferService(transferChannel);
            result = transferService.transfer(request);
        } catch (Exception e) {
            log.error("调用转账通道异常, detailId={}, channel={}", detail.getId(), transferChannel, e);
            result = TransferResult.fail(transferNo, "通道调用异常: " + e.getMessage());
        }

        handleTransferResult(detail, result);

        checkAndUpdateSettlementStatus(detail.getSettlementId());
    }

    private String determineTransferChannel(SettlementRecord settlement, PaySplitDetail detail) {
        if (detail.getReceiverAccount() != null && detail.getReceiverAccount().startsWith("62")) {
            return "UNION_PAY";
        }
        String account = detail.getReceiverAccount();
        if (account != null && (account.contains("@") || account.matches("^1[3-9]\\d{9}$"))) {
            return "ALIPAY";
        }
        if (settlement != null && settlement.getPayChannel() != null) {
            String channel = settlement.getPayChannel();
            if ("ALIPAY".equals(channel) || "WECHAT_PAY".equals(channel) || "UNION_PAY".equals(channel)) {
                return channel;
            }
        }
        return "ALIPAY";
    }

    private TransferRequest buildTransferRequest(PaySplitDetail detail, SettlementRecord settlement, String channel) {
        TransferRequest request = new TransferRequest();
        request.setTransferNo(detail.getTransferNo());
        request.setChannel(channel);
        request.setReceiverAccount(detail.getReceiverAccount());
        request.setReceiverName(detail.getReceiverName());
        request.setAmount(detail.getSplitAmount() != null
                ? detail.getSplitAmount().multiply(new BigDecimal("100"))
                : BigDecimal.ZERO);
        if (settlement != null) {
            request.setBankName(settlement.getBankName());
        }
        request.setRemark(detail.getRemark() != null ? detail.getRemark() : "分账打款");
        request.setSourceType("SETTLEMENT_SPLIT");
        request.setSourceNo(detail.getSplitDetailNo());
        return request;
    }

    private void handleTransferResult(PaySplitDetail detail, TransferResult result) {
        Integer newRetryCount = detail.getTransferRetryCount() == null ? 1 : detail.getTransferRetryCount() + 1;
        String status = result.getStatus();

        if (TransferResult.STATUS_SUCCESS.equals(status)) {
            detail.setTransferStatus(TRANSFER_STATUS_SUCCESS);
            detail.setChannelTransferNo(result.getChannelTransferNo());
            detail.setTransferTime(result.getCompleteTime() != null ? result.getCompleteTime() : LocalDateTime.now());
            detail.setTransferFailReason(null);
            detail.setStatus(1);
            detail.setSettleTime(detail.getTransferTime());
            log.info("分账明细打款成功: detailId={}, transferNo={}, channelTransferNo={}",
                    detail.getId(), detail.getTransferNo(), result.getChannelTransferNo());
        } else if (TransferResult.STATUS_PROCESSING.equals(status)) {
            detail.setTransferStatus(TRANSFER_STATUS_PROCESSING);
            detail.setChannelTransferNo(result.getChannelTransferNo());
            detail.setTransferRetryCount(newRetryCount);
            log.info("分账明细打款处理中: detailId={}, transferNo={}, channelTransferNo={}",
                    detail.getId(), detail.getTransferNo(), result.getChannelTransferNo());
        } else {
            detail.setTransferStatus(TRANSFER_STATUS_FAIL);
            detail.setTransferFailReason(result.getFailReason());
            detail.setTransferRetryCount(newRetryCount);
            if (newRetryCount < MAX_RETRY_COUNT) {
                detail.setNextTransferRetryTime(calculateNextRetryTime(newRetryCount));
            }
            log.warn("分账明细打款失败: detailId={}, transferNo={}, retryCount={}, failReason={}",
                    detail.getId(), detail.getTransferNo(), newRetryCount, result.getFailReason());
        }

        paySplitDetailMapper.updateById(detail);
    }

    @Transactional(rollbackFor = Exception.class)
    public void checkAndUpdateSettlementStatus(Long settlementId) {
        SettlementRecord settlement = this.getById(settlementId);
        if (settlement == null) {
            log.warn("结算记录不存在, settlementId={}", settlementId);
            return;
        }

        LambdaQueryWrapper<PaySplitDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaySplitDetail::getSettlementId, settlementId);
        List<PaySplitDetail> details = paySplitDetailMapper.selectList(wrapper);

        if (details.isEmpty()) {
            return;
        }

        int totalCount = details.size();
        int successCount = 0;
        int processingCount = 0;
        int failCount = 0;
        int pendingCount = 0;
        StringBuilder failReasons = new StringBuilder();

        for (PaySplitDetail detail : details) {
            Integer status = detail.getTransferStatus();
            if (status == null) {
                pendingCount++;
            } else {
                switch (status) {
                    case 2:
                        successCount++;
                        break;
                    case 1:
                        processingCount++;
                        break;
                    case 3:
                        failCount++;
                        if (detail.getTransferFailReason() != null) {
                            if (failReasons.length() > 0) {
                                failReasons.append("; ");
                            }
                            failReasons.append(detail.getSplitDetailNo()).append(":").append(detail.getTransferFailReason());
                        }
                        break;
                    default:
                        pendingCount++;
                        break;
                }
            }
        }

        Integer oldStatus = settlement.getSettleStatus();
        String oldFailReason = settlement.getFailReason();
        Integer newStatus;
        String newFailReason = oldFailReason;
        boolean needUpdate = false;

        if (successCount == totalCount) {
            newStatus = 2;
            if (settlement.getSettleTime() == null) {
                settlement.setSettleTime(LocalDateTime.now());
                needUpdate = true;
            }
            newFailReason = null;
            log.info("结算全部打款成功: settlementId={}, settlementNo={}, count={}",
                    settlementId, settlement.getSettlementNo(), totalCount);
        } else if (processingCount > 0 || pendingCount > 0) {
            newStatus = 1;
            if (failCount > 0) {
                newFailReason = "部分明细打款失败待重试: " + failReasons;
            }
            log.info("结算部分处理中: settlementId={}, settlementNo={}, success={}, processing={}, fail={}, pending={}",
                    settlementId, settlement.getSettlementNo(), successCount, processingCount, failCount, pendingCount);
        } else if (successCount > 0 && failCount > 0) {
            newStatus = 1;
            newFailReason = "部分打款成功，部分失败: " + failReasons;
            if (settlement.getSettleTime() == null) {
                settlement.setSettleTime(LocalDateTime.now());
                needUpdate = true;
            }
            log.warn("结算部分成功部分失败: settlementId={}, settlementNo={}, success={}, fail={}",
                    settlementId, settlement.getSettlementNo(), successCount, failCount);
        } else {
            newStatus = 3;
            newFailReason = "全部明细打款失败: " + failReasons;
            log.error("结算全部打款失败: settlementId={}, settlementNo={}, failCount={}",
                    settlementId, settlement.getSettlementNo(), failCount);
        }

        if (!newStatus.equals(oldStatus)) {
            settlement.setSettleStatus(newStatus);
            needUpdate = true;
        }
        if ((newFailReason == null && oldFailReason != null)
                || (newFailReason != null && !newFailReason.equals(oldFailReason))) {
            settlement.setFailReason(newFailReason);
            needUpdate = true;
        }

        if (needUpdate) {
            this.updateById(settlement);
            log.info("结算状态更新: settlementId={}, {} -> {}, failReason={}",
                    settlementId, oldStatus, newStatus, newFailReason);
        }
    }

    private LocalDateTime calculateNextRetryTime(int retryCount) {
        int minutes = (int) Math.pow(2, retryCount - 1);
        return LocalDateTime.now().plusMinutes(minutes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryFailedSettlement() {
        log.info("手动触发重试所有失败的分账打款明细");

        LambdaQueryWrapper<PaySplitDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaySplitDetail::getTransferStatus, TRANSFER_STATUS_FAIL)
                .and(w -> w.lt(PaySplitDetail::getTransferRetryCount, MAX_RETRY_COUNT)
                        .or().isNull(PaySplitDetail::getTransferRetryCount));

        List<PaySplitDetail> details = paySplitDetailMapper.selectList(wrapper);
        if (details.isEmpty()) {
            log.info("没有可重试的分账打款失败明细");
            return;
        }

        log.info("待重试分账打款明细数量: {}", details.size());

        for (PaySplitDetail detail : details) {
            try {
                detail.setNextTransferRetryTime(LocalDateTime.now());
                paySplitDetailMapper.updateById(detail);
                processSplitDetailTransfer(detail);
            } catch (Exception e) {
                log.error("重试分账打款明细异常, id={}", detail.getId(), e);
            }
        }

        log.info("手动重试分账打款失败明细完成");
    }

    @Override
    public SettlementVO getSettlementDetail(Long id) {
        SettlementRecord record = this.getById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "结算记录不存在");
        }
        SettlementVO vo = convertToVO(record);
        List<PaySplitDetail> details = splitEngineService.getSplitDetailsBySettlementId(id);
        vo.setSplitDetails(details);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmSettle(Long id) {
        SettlementRecord record = this.getById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "结算记录不存在");
        }
        if (record.getSettleStatus() != 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只有待结算状态的记录才能确认打款");
        }

        log.info("人工确认结算打款: id={}, settlementNo={}", id, record.getSettlementNo());

        processSettlement(record);
    }

    @Override
    public IPage<PaySplitDetail> listSettlementDetails(Long current, Long size, Long settlementId) {
        Map<String, Object> params = new HashMap<>();
        params.put("settlementId", settlementId);
        return splitEngineService.listSplitDetails(current, size, params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retrySettlement(Long id) {
        SettlementRecord record = this.getById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "结算记录不存在");
        }

        log.info("重试单个结算下的分账明细打款: id={}, settlementNo={}", id, record.getSettlementNo());

        LambdaQueryWrapper<PaySplitDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaySplitDetail::getSettlementId, id)
                .and(w -> w.eq(PaySplitDetail::getTransferStatus, TRANSFER_STATUS_FAIL)
                        .or().eq(PaySplitDetail::getTransferStatus, TRANSFER_STATUS_PENDING)
                        .or().isNull(PaySplitDetail::getTransferStatus));

        List<PaySplitDetail> details = paySplitDetailMapper.selectList(wrapper);
        if (details.isEmpty()) {
            log.info("结算 {} 下没有可重试的分账打款明细", record.getSettlementNo());
            return;
        }

        int retryCount = 0;
        for (PaySplitDetail detail : details) {
            Integer detailRetryCount = detail.getTransferRetryCount();
            if (detailRetryCount != null && detailRetryCount >= MAX_RETRY_COUNT) {
                log.warn("分账明细 {} 已达最大重试次数, 跳过", detail.getSplitDetailNo());
                continue;
            }
            try {
                detail.setNextTransferRetryTime(LocalDateTime.now());
                paySplitDetailMapper.updateById(detail);
                processSplitDetailTransfer(detail);
                retryCount++;
            } catch (Exception e) {
                log.error("重试分账打款明细异常, id={}", detail.getId(), e);
            }
        }

        log.info("结算 {} 重试完成, 共触发{}条分账明细打款", record.getSettlementNo(), retryCount);
    }

    private SettlementVO convertToVO(SettlementRecord record) {
        SettlementVO vo = BeanUtil.copyProperties(record, SettlementVO.class);
        String statusDesc;
        switch (record.getSettleStatus()) {
            case 0:
                statusDesc = "待结算";
                break;
            case 1:
                statusDesc = "结算中";
                break;
            case 2:
                statusDesc = "已结算";
                break;
            case 3:
                statusDesc = "结算失败";
                break;
            default:
                statusDesc = "未知";
        }
        vo.setSettleStatusDesc(statusDesc);
        return vo;
    }
}
