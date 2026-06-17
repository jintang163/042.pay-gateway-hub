package com.payhub.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.mapper.PayOrderMapper;
import com.payhub.settlement.dto.SettlementVO;
import com.payhub.settlement.entity.SettlementRecord;
import com.payhub.settlement.mapper.SettlementRecordMapper;
import com.payhub.settlement.service.SettlementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SettlementServiceImpl extends ServiceImpl<SettlementRecordMapper, SettlementRecord> implements SettlementService {

    @Autowired
    private PayOrderMapper payOrderMapper;

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

        Map<String, List<PayOrder>> ordersByMerchant = orders.stream()
                .collect(Collectors.groupingBy(PayOrder::getMerchantNo));

        for (Map.Entry<String, List<PayOrder>> entry : ordersByMerchant.entrySet()) {
            String merchantNo = entry.getKey();
            List<PayOrder> merchantOrders = entry.getValue();

            LambdaQueryWrapper<SettlementRecord> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(SettlementRecord::getMerchantNo, merchantNo)
                    .eq(SettlementRecord::getSettleDate, settleDate);
            SettlementRecord existRecord = this.getOne(existWrapper);
            if (existRecord != null) {
                log.warn("商户 {} 结算日期 {} 的结算记录已存在, 跳过", merchantNo, settleDate);
                continue;
            }

            BigDecimal totalAmount = merchantOrders.stream()
                    .map(PayOrder::getPayAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal feeAmount = merchantOrders.stream()
                    .map(order -> order.getFeeAmount() != null ? order.getFeeAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal actualSettleAmount = totalAmount.subtract(feeAmount);

            SettlementRecord record = new SettlementRecord();
            record.setSettlementNo(OrderNoGenerator.generateWithPrefix("ST"));
            record.setMerchantNo(merchantNo);
            record.setSettleDate(settleDate);
            record.setTotalAmount(totalAmount);
            record.setFeeAmount(feeAmount);
            record.setActualSettleAmount(actualSettleAmount);
            record.setOrderCount(merchantOrders.size());
            record.setSettleStatus(0);

            this.save(record);
            log.info("生成结算记录成功: settlementNo={}, merchantNo={}, totalAmount={}, feeAmount={}, actualSettleAmount={}",
                    record.getSettlementNo(), merchantNo, totalAmount, feeAmount, actualSettleAmount);
        }

        log.info("结算记录生成完成, 结算日期: {}", settleDate);
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

    private SettlementVO convertToVO(SettlementRecord record) {
        SettlementVO vo = BeanUtil.copyProperties(record, SettlementVO.class);
        vo.setSettleStatusDesc(record.getSettleStatus() == 1 ? "已结算" : "待结算");
        return vo;
    }
}
