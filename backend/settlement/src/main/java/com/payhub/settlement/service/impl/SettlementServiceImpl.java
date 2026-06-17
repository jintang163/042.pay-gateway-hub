package com.payhub.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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

    private static final int MAX_RETRY_COUNT = 5;

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
        log.info("开始执行批量结算打款任务");

        LambdaQueryWrapper<SettlementRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(SettlementRecord::getSettleStatus, 0)
                        .or()
                        .and(w2 -> w2.eq(SettlementRecord::getSettleStatus, 3)
                                .lt(SettlementRecord::getNextRetryTime, LocalDateTime.now())
                                .lt(SettlementRecord::getRetryCount, MAX_RETRY_COUNT)))
                .orderByAsc(SettlementRecord::getId)
                .last("LIMIT 100");

        List<SettlementRecord> records = this.list(wrapper);
        if (records.isEmpty()) {
            log.info("没有需要处理的结算记录");
            return;
        }

        log.info("待处理结算记录数量: {}", records.size());

        for (SettlementRecord record : records) {
            try {
                processSettlement(record);
            } catch (Exception e) {
                log.error("处理结算记录异常, id={}, settlementNo={}", record.getId(), record.getSettlementNo(), e);
            }
        }

        log.info("批量结算打款任务执行完成");
    }

    @Transactional(rollbackFor = Exception.class)
    public void processSettlement(SettlementRecord record) {
        log.info("开始处理结算记录: id={}, settlementNo={}", record.getId(), record.getSettlementNo());

        record.setSettleStatus(1);
        this.updateById(record);

        boolean success = mockTransfer(record);

        if (success) {
            record.setSettleStatus(2);
            record.setSettleTime(LocalDateTime.now());
            record.setFailReason(null);
            this.updateById(record);

            splitEngineService.updateStatusBySettlementId(record.getId(), 1);

            log.info("结算成功: id={}, settlementNo={}, amount={}",
                    record.getId(), record.getSettlementNo(), record.getActualSettleAmount());
        } else {
            record.setSettleStatus(3);
            record.setFailReason("模拟转账失败");
            record.setRetryCount(record.getRetryCount() == null ? 1 : record.getRetryCount() + 1);

            if (record.getRetryCount() < MAX_RETRY_COUNT) {
                record.setNextRetryTime(calculateNextRetryTime(record.getRetryCount()));
            }

            this.updateById(record);

            log.warn("结算失败: id={}, settlementNo={}, retryCount={}, nextRetryTime={}",
                    record.getId(), record.getSettlementNo(), record.getRetryCount(), record.getNextRetryTime());
        }
    }

    private boolean mockTransfer(SettlementRecord record) {
        log.info("模拟转账: settlementNo={}, amount={}, account={}",
                record.getSettlementNo(), record.getActualSettleAmount(), record.getBankAccount());
        return Math.random() > 0.2;
    }

    private LocalDateTime calculateNextRetryTime(int retryCount) {
        int minutes = (int) Math.pow(2, retryCount - 1);
        return LocalDateTime.now().plusMinutes(minutes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryFailedSettlement() {
        log.info("手动触发重试所有失败结算");

        LambdaQueryWrapper<SettlementRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SettlementRecord::getSettleStatus, 3)
                .lt(SettlementRecord::getRetryCount, MAX_RETRY_COUNT);

        List<SettlementRecord> records = this.list(wrapper);
        if (records.isEmpty()) {
            log.info("没有可重试的失败结算记录");
            return;
        }

        log.info("待重试结算记录数量: {}", records.size());

        for (SettlementRecord record : records) {
            try {
                record.setNextRetryTime(LocalDateTime.now());
                this.updateById(record);
                processSettlement(record);
            } catch (Exception e) {
                log.error("重试结算记录异常, id={}", record.getId(), e);
            }
        }

        log.info("手动重试失败结算完成");
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
        if (record.getSettleStatus() != 3) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "只有失败状态的记录才能重试");
        }
        if (record.getRetryCount() != null && record.getRetryCount() >= MAX_RETRY_COUNT) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "已达最大重试次数");
        }

        log.info("重试单个结算: id={}, settlementNo={}", id, record.getSettlementNo());

        record.setNextRetryTime(LocalDateTime.now());
        this.updateById(record);
        processSettlement(record);
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
