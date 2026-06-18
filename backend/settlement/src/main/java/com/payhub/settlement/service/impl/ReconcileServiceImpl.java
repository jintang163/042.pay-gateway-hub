package com.payhub.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.channel.dto.ChannelReconcileBill;
import com.payhub.channel.strategy.PayChannelStrategy;
import com.payhub.channel.strategy.PayChannelStrategyFactory;
import com.payhub.common.enums.PayStatusEnum;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.mapper.PayOrderMapper;
import com.payhub.settlement.dto.ReconcileVO;
import com.payhub.settlement.entity.ErrorOrder;
import com.payhub.settlement.entity.ReconcileDetail;
import com.payhub.settlement.entity.ReconcileRecord;
import com.payhub.settlement.enums.*;
import com.payhub.settlement.mapper.ErrorOrderMapper;
import com.payhub.settlement.mapper.ReconcileDetailMapper;
import com.payhub.settlement.mapper.ReconcileRecordMapper;
import com.payhub.settlement.service.ReconcileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
public class ReconcileServiceImpl extends ServiceImpl<ReconcileRecordMapper, ReconcileRecord> implements ReconcileService {

    @Autowired
    private PayOrderMapper payOrderMapper;

    @Autowired
    private ReconcileDetailMapper reconcileDetailMapper;

    @Autowired
    private ErrorOrderMapper errorOrderMapper;

    @Autowired
    private PayChannelStrategyFactory payChannelStrategyFactory;

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

        if (StrUtil.isBlank(payChannel) || reconcileDate == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "支付渠道和对账日期不能为空");
        }
        if (!payChannelStrategyFactory.containsStrategy(payChannel)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的支付渠道: " + payChannel);
        }

        LambdaQueryWrapper<ReconcileRecord> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(ReconcileRecord::getPayChannel, payChannel)
                .eq(ReconcileRecord::getReconcileDate, reconcileDate);
        ReconcileRecord existRecord = this.getOne(existWrapper);
        if (existRecord != null) {
            log.warn("支付渠道 {} 对账日期 {} 的对账记录已存在, 先删除旧记录和差异明细", payChannel, reconcileDate);
            LambdaQueryWrapper<ReconcileDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.eq(ReconcileDetail::getReconcileNo, existRecord.getReconcileNo());
            reconcileDetailMapper.delete(detailWrapper);
            this.removeById(existRecord.getId());
        }

        ReconcileRecord record = new ReconcileRecord();
        record.setReconcileNo(OrderNoGenerator.generateWithPrefix("RC"));
        record.setPayChannel(payChannel);
        record.setReconcileDate(reconcileDate);
        record.setReconcileStatus(0);
        record.setTotalCount(0);
        record.setMatchCount(0);
        record.setMismatchCount(0);
        this.save(record);

        try {
            LocalDateTime startTime = LocalDateTime.of(reconcileDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(reconcileDate, LocalTime.MAX);

            LambdaQueryWrapper<PayOrder> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(PayOrder::getPayChannel, payChannel)
                    .eq(PayOrder::getPayStatus, PayStatusEnum.SUCCESS.getCode())
                    .between(PayOrder::getPayTime, startTime, endTime);
            List<PayOrder> localOrders = payOrderMapper.selectList(orderWrapper);

            log.info("本地订单数量: {}, 渠道: {}, 日期: {}", localOrders.size(), payChannel, reconcileDate);

            Map<String, PayOrder> localOrderMap = new HashMap<>();
            Map<String, PayOrder> localChannelTradeNoMap = new HashMap<>();
            Map<String, List<PayOrder>> localOrdersByMerchant = new HashMap<>();
            for (PayOrder order : localOrders) {
                localOrderMap.put(order.getOrderNo(), order);
                if (StrUtil.isNotBlank(order.getChannelTradeNo())) {
                    localChannelTradeNoMap.put(order.getChannelTradeNo(), order);
                }
                String mchNo = StrUtil.isNotBlank(order.getMerchantNo()) ? order.getMerchantNo() : "UNKNOWN";
                localOrdersByMerchant.computeIfAbsent(mchNo, k -> new ArrayList<>()).add(order);
            }

            PayChannelStrategy strategy = payChannelStrategyFactory.getStrategy(payChannel);

            List<ChannelReconcileBill.ChannelReconcileItem> allChannelItems = new ArrayList<>();

            if (localOrdersByMerchant.isEmpty()) {
                ChannelReconcileBill channelBill = strategy.downloadReconcileBill(reconcileDate, null);
                if (channelBill != null && channelBill.getItems() != null) {
                    allChannelItems.addAll(channelBill.getItems());
                }
            } else {
                for (Map.Entry<String, List<PayOrder>> entry : localOrdersByMerchant.entrySet()) {
                    String merchantNo = entry.getKey();
                    try {
                        ChannelReconcileBill channelBill = strategy.downloadReconcileBill(reconcileDate, merchantNo);
                        if (channelBill != null && channelBill.getItems() != null) {
                            allChannelItems.addAll(channelBill.getItems());
                        }
                        log.info("拉取商户 {} 对账单成功, 笔数: {}", merchantNo,
                                channelBill != null && channelBill.getItems() != null ? channelBill.getItems().size() : 0);
                    } catch (Exception e) {
                        log.warn("拉取商户 {} 对账单失败: {}", merchantNo, e.getMessage());
                    }
                }
            }

            log.info("渠道对账单总数量: {}, 渠道: {}, 日期: {}", allChannelItems.size(), payChannel, reconcileDate);

            Map<String, ChannelReconcileBill.ChannelReconcileItem> channelItemMap = new HashMap<>();
            for (ChannelReconcileBill.ChannelReconcileItem item : allChannelItems) {
                if (StrUtil.isNotBlank(item.getChannelTradeNo())) {
                    channelItemMap.put(item.getChannelTradeNo(), item);
                }
                if (StrUtil.isNotBlank(item.getMerchantOrderNo()) && !channelItemMap.containsKey(item.getMerchantOrderNo())) {
                    channelItemMap.put(item.getMerchantOrderNo(), item);
                }
            }

            int matchCount = 0;
            int mismatchCount = 0;
            List<ReconcileDetail> mismatchDetails = new ArrayList<>();

            for (PayOrder localOrder : localOrders) {
                ChannelReconcileBill.ChannelReconcileItem channelItem = null;
                if (StrUtil.isNotBlank(localOrder.getChannelTradeNo())) {
                    channelItem = channelItemMap.get(localOrder.getChannelTradeNo());
                }
                if (channelItem == null && StrUtil.isNotBlank(localOrder.getMerchantOrderNo())) {
                    channelItem = channelItemMap.get(localOrder.getMerchantOrderNo());
                }

                if (channelItem == null) {
                    ReconcileDetail detail = buildReconcileDetail(record.getReconcileNo(), reconcileDate, payChannel,
                            ReconcileDiffTypeEnum.SHORT_FUND, localOrder, null);
                    mismatchDetails.add(detail);
                    mismatchCount++;
                } else {
                    boolean amountMatch = compareAmount(
                            localOrder.getActualAmount() != null ? localOrder.getActualAmount() : localOrder.getPayAmount(),
                            channelItem.getTradeAmount());
                    boolean statusMatch = compareStatus(localOrder.getPayStatus(), channelItem.getTradeStatus());

                    if (!amountMatch || !statusMatch) {
                        ReconcileDiffTypeEnum diffType;
                        if (!amountMatch && !statusMatch) {
                            diffType = ReconcileDiffTypeEnum.AMOUNT_MISMATCH;
                        } else if (!amountMatch) {
                            diffType = ReconcileDiffTypeEnum.AMOUNT_MISMATCH;
                        } else {
                            diffType = ReconcileDiffTypeEnum.STATUS_MISMATCH;
                        }
                        ReconcileDetail detail = buildReconcileDetail(record.getReconcileNo(), reconcileDate, payChannel,
                                diffType, localOrder, channelItem);
                        mismatchDetails.add(detail);
                        mismatchCount++;
                    } else {
                        matchCount++;
                    }

                    if (StrUtil.isNotBlank(localOrder.getChannelTradeNo())) {
                        channelItemMap.remove(localOrder.getChannelTradeNo());
                    }
                    if (StrUtil.isNotBlank(localOrder.getMerchantOrderNo())) {
                        channelItemMap.remove(localOrder.getMerchantOrderNo());
                    }
                }
            }

            Set<String> processedKeys = new HashSet<>();
            for (Map.Entry<String, ChannelReconcileBill.ChannelReconcileItem> entry : channelItemMap.entrySet()) {
                String key = entry.getKey();
                ChannelReconcileBill.ChannelReconcileItem item = entry.getValue();

                String uniqueKey = StrUtil.isNotBlank(item.getChannelTradeNo())
                        ? item.getChannelTradeNo()
                        : key;
                if (processedKeys.contains(uniqueKey)) {
                    continue;
                }
                processedKeys.add(uniqueKey);

                boolean foundInLocal = false;
                if (StrUtil.isNotBlank(item.getChannelTradeNo())
                        && localChannelTradeNoMap.containsKey(item.getChannelTradeNo())) {
                    foundInLocal = true;
                }
                if (StrUtil.isNotBlank(item.getMerchantOrderNo())
                        && localOrderMap.containsKey(item.getMerchantOrderNo())) {
                    foundInLocal = true;
                }

                if (!foundInLocal) {
                    ReconcileDetail detail = buildReconcileDetail(record.getReconcileNo(), reconcileDate, payChannel,
                            ReconcileDiffTypeEnum.LONG_FUND, null, item);
                    mismatchDetails.add(detail);
                    mismatchCount++;
                }
            }

            int totalCount = matchCount + mismatchCount;

            if (CollUtil.isNotEmpty(mismatchDetails)) {
                for (ReconcileDetail detail : mismatchDetails) {
                    reconcileDetailMapper.insert(detail);

                    ErrorOrder errorOrder = buildAutoErrorOrder(record.getReconcileNo(), detail);
                    errorOrderMapper.insert(errorOrder);

                    detail.setErrorOrderNo(errorOrder.getErrorNo());
                    detail.setHandleStatus(ReconcileHandleStatusEnum.PROCESSING.getCode());
                    reconcileDetailMapper.updateById(detail);
                }
                log.info("保存差异明细 {} 条并自动生成差错单, 对账单号: {}", mismatchDetails.size(), record.getReconcileNo());
            }

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
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException(ResultCode.FAIL, "对账执行异常: " + e.getMessage());
        }
    }

    private ReconcileDetail buildReconcileDetail(String reconcileNo, LocalDate reconcileDate, String payChannel,
                                                  ReconcileDiffTypeEnum diffType, PayOrder localOrder,
                                                  ChannelReconcileBill.ChannelReconcileItem channelItem) {
        ReconcileDetail detail = new ReconcileDetail();
        detail.setDetailNo(OrderNoGenerator.generateWithPrefix("RD"));
        detail.setReconcileNo(reconcileNo);
        detail.setReconcileDate(reconcileDate);
        detail.setPayChannel(payChannel);
        detail.setDiffType(diffType.getCode());
        detail.setHandleStatus(ReconcileHandleStatusEnum.PENDING.getCode());

        if (localOrder != null) {
            detail.setOrderNo(localOrder.getOrderNo());
            detail.setMerchantNo(localOrder.getMerchantNo());
            detail.setChannelTradeNo(localOrder.getChannelTradeNo());
            detail.setLocalAmount(localOrder.getActualAmount() != null
                    ? localOrder.getActualAmount() : localOrder.getPayAmount());
            detail.setLocalStatus(localOrder.getPayStatus());
            detail.setLocalPayTime(localOrder.getPayTime());
        }

        if (channelItem != null) {
            if (StrUtil.isBlank(detail.getChannelTradeNo())) {
                detail.setChannelTradeNo(channelItem.getChannelTradeNo());
            }
            detail.setChannelAmount(channelItem.getTradeAmount());
            detail.setChannelStatus(channelItem.getTradeStatus());
            detail.setChannelPayTime(channelItem.getTradeTime());
        }

        if (detail.getLocalAmount() != null && detail.getChannelAmount() != null) {
            detail.setDiffAmount(detail.getLocalAmount().subtract(detail.getChannelAmount()));
        } else if (detail.getLocalAmount() != null) {
            detail.setDiffAmount(detail.getLocalAmount());
        } else if (detail.getChannelAmount() != null) {
            detail.setDiffAmount(detail.getChannelAmount().negate());
        }

        return detail;
    }

    private boolean compareAmount(BigDecimal localAmount, BigDecimal channelAmount) {
        if (localAmount == null && channelAmount == null) {
            return true;
        }
        if (localAmount == null || channelAmount == null) {
            return false;
        }
        return localAmount.compareTo(channelAmount) == 0;
    }

    private boolean compareStatus(Integer localPayStatus, String channelTradeStatus) {
        if (localPayStatus == null && StrUtil.isBlank(channelTradeStatus)) {
            return true;
        }
        if (localPayStatus == null || StrUtil.isBlank(channelTradeStatus)) {
            return false;
        }
        PayStatusEnum localStatusEnum = PayStatusEnum.getByCode(localPayStatus);
        if (localStatusEnum == null) {
            return false;
        }
        String normalizedChannel = channelTradeStatus.toUpperCase().trim();
        switch (localStatusEnum) {
            case SUCCESS:
                return "SUCCESS".equals(normalizedChannel) || "支付成功".equals(channelTradeStatus);
            case FAIL:
                return "FAILED".equals(normalizedChannel) || "支付失败".equals(channelTradeStatus);
            case PENDING:
                return "PENDING".equals(normalizedChannel) || "待支付".equals(channelTradeStatus);
            case CLOSED:
                return "CLOSED".equals(normalizedChannel) || "订单关闭".equals(channelTradeStatus);
            case REFUNDING:
                return "REFUNDING".equals(normalizedChannel) || "退款中".equals(channelTradeStatus);
            case REFUNDED:
                return "REFUNDED".equals(normalizedChannel) || "已退款".equals(channelTradeStatus);
            default:
                return false;
        }
    }

    private ErrorOrder buildAutoErrorOrder(String reconcileNo, ReconcileDetail detail) {
        ErrorOrder errorOrder = new ErrorOrder();
        errorOrder.setErrorNo(OrderNoGenerator.generateWithPrefix("ERR"));
        errorOrder.setReconcileNo(reconcileNo);
        errorOrder.setReconcileDetailId(detail.getId());
        errorOrder.setPayChannel(detail.getPayChannel());
        errorOrder.setErrorType(detail.getDiffType());
        errorOrder.setOrderNo(detail.getOrderNo());
        errorOrder.setMerchantNo(detail.getMerchantNo());
        errorOrder.setChannelTradeNo(detail.getChannelTradeNo());
        errorOrder.setOrderAmount(detail.getLocalAmount());
        errorOrder.setActualAmount(detail.getChannelAmount());
        errorOrder.setDiffAmount(detail.getDiffAmount());
        errorOrder.setErrorStatus(ErrorStatusEnum.PENDING.getCode());
        errorOrder.setAuditStatus(AuditStatusEnum.PENDING.getCode());
        errorOrder.setApplyUserId("SYSTEM");
        errorOrder.setApplyUserName("系统自动");
        errorOrder.setApplyTime(LocalDateTime.now());
        errorOrder.setApplyRemark("对账差异自动生成差错单");

        ReconcileDiffTypeEnum diffTypeEnum = ReconcileDiffTypeEnum.getByCode(detail.getDiffType());
        if (diffTypeEnum != null) {
            switch (diffTypeEnum) {
                case LONG_FUND:
                    errorOrder.setHandleType(ErrorHandleTypeEnum.IGNORE.getCode());
                    break;
                case SHORT_FUND:
                    errorOrder.setHandleType(ErrorHandleTypeEnum.SUPPLEMENT.getCode());
                    break;
                case AMOUNT_MISMATCH:
                    errorOrder.setHandleType(ErrorHandleTypeEnum.ADJUST.getCode());
                    break;
                case STATUS_MISMATCH:
                    errorOrder.setHandleType(ErrorHandleTypeEnum.ADJUST.getCode());
                    break;
                default:
                    break;
            }
        }

        return errorOrder;
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
