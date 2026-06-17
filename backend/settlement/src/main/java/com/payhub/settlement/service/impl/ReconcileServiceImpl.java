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
import com.payhub.settlement.dto.ReconcileVO;
import com.payhub.settlement.entity.ReconcileRecord;
import com.payhub.settlement.mapper.ReconcileRecordMapper;
import com.payhub.settlement.service.ReconcileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class ReconcileServiceImpl extends ServiceImpl<ReconcileRecordMapper, ReconcileRecord> implements ReconcileService {

    @Autowired
    private PayOrderMapper payOrderMapper;

    @Override
    public ReconcileVO getByReconcileNo(String reconcileNo) {
        LambdaQueryWrapper<ReconcileRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconcileRecord::getReconcileNo, reconcileNo);
        ReconcileRecord record = this.getOne(wrapper);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "对账记录不存在");
        }
        return convertToVO(record);
    }

    @Override
    public IPage<ReconcileVO> listPage(Long current, Long size, Map<String, Object> params) {
        Page<ReconcileRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<ReconcileRecord> wrapper = new LambdaQueryWrapper<>();
        if (params != null) {
            if (params.get("payChannel") != null) {
                wrapper.eq(ReconcileRecord::getPayChannel, params.get("payChannel"));
            }
            if (params.get("reconcileDate") != null) {
                wrapper.eq(ReconcileRecord::getReconcileDate, params.get("reconcileDate"));
            }
            if (params.get("reconcileStatus") != null) {
                wrapper.eq(ReconcileRecord::getReconcileStatus, params.get("reconcileStatus"));
            }
        }
        wrapper.orderByDesc(ReconcileRecord::getReconcileDate, ReconcileRecord::getId);
        IPage<ReconcileRecord> recordPage = this.page(page, wrapper);
        return recordPage.convert(this::convertToVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeReconcile(String payChannel, LocalDate reconcileDate) {
        log.info("开始执行对账, 支付渠道: {}, 对账日期: {}", payChannel, reconcileDate);

        LambdaQueryWrapper<ReconcileRecord> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(ReconcileRecord::getPayChannel, payChannel)
                .eq(ReconcileRecord::getReconcileDate, reconcileDate);
        ReconcileRecord existRecord = this.getOne(existWrapper);
        if (existRecord != null) {
            log.warn("支付渠道 {} 对账日期 {} 的对账记录已存在, 先删除旧记录", payChannel, reconcileDate);
            this.removeById(existRecord.getId());
        }

        ReconcileRecord record = new ReconcileRecord();
        record.setReconcileNo(OrderNoGenerator.generateWithPrefix("RC"));
        record.setPayChannel(payChannel);
        record.setReconcileDate(reconcileDate);
        record.setReconcileStatus(0);
        this.save(record);

        try {
            LocalDateTime startTime = LocalDateTime.of(reconcileDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(reconcileDate, LocalTime.MAX);

            LambdaQueryWrapper<PayOrder> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(PayOrder::getPayChannel, payChannel)
                    .eq(PayOrder::getPayStatus, 1)
                    .between(PayOrder::getPayTime, startTime, endTime);
            List<PayOrder> localOrders = payOrderMapper.selectList(orderWrapper);

            int totalCount = localOrders.size();
            Set<String> localTradeNos = new HashSet<>();
            for (PayOrder order : localOrders) {
                if (order.getChannelTradeNo() != null) {
                    localTradeNos.add(order.getChannelTradeNo());
                }
            }

            int matchCount = localTradeNos.size();
            int mismatchCount = totalCount - matchCount;

            record.setTotalCount(totalCount);
            record.setMatchCount(matchCount);
            record.setMismatchCount(mismatchCount);
            record.setReconcileStatus(1);
            this.updateById(record);

            log.info("对账完成, 支付渠道: {}, 对账日期: {}, 总笔数: {}, 匹配笔数: {}, 不匹配笔数: {}",
                    payChannel, reconcileDate, totalCount, matchCount, mismatchCount);
        } catch (Exception e) {
            log.error("对账执行异常, 支付渠道: {}, 对账日期: {}", payChannel, reconcileDate, e);
            record.setReconcileStatus(2);
            this.updateById(record);
            throw new BusinessException(ResultCode.FAIL, "对账执行异常: " + e.getMessage());
        }
    }

    private ReconcileVO convertToVO(ReconcileRecord record) {
        ReconcileVO vo = BeanUtil.copyProperties(record, ReconcileVO.class);
        String statusDesc;
        switch (record.getReconcileStatus()) {
            case 1:
                statusDesc = "完成";
                break;
            case 2:
                statusDesc = "异常";
                break;
            default:
                statusDesc = "处理中";
        }
        vo.setReconcileStatusDesc(statusDesc);
        return vo;
    }
}
