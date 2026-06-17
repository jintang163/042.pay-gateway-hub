package com.payhub.pay.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payhub.common.enums.PayStatusEnum;
import com.payhub.pay.dto.vo.ChannelDistributionVO;
import com.payhub.pay.dto.vo.OverviewStatsVO;
import com.payhub.pay.dto.vo.SuccessRateTrendVO;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.mapper.PayOrderMapper;
import com.payhub.pay.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private PayOrderMapper payOrderMapper;

    @Autowired(required = false)
    private com.payhub.merchant.service.MerchantInfoService merchantInfoService;

    @Override
    public OverviewStatsVO getOverviewStats(String merchantNo) {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        LambdaQueryWrapper<PayOrder> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(PayOrder::getCreatedAt, todayStart)
                .le(PayOrder::getCreatedAt, todayEnd);
        if (StrUtil.isNotBlank(merchantNo)) {
            todayWrapper.eq(PayOrder::getMerchantNo, merchantNo);
        }
        List<PayOrder> todayOrders = payOrderMapper.selectList(todayWrapper);

        BigDecimal todayAmount = BigDecimal.ZERO;
        long successCount = 0;
        for (PayOrder order : todayOrders) {
            if (PayStatusEnum.SUCCESS.getCode().equals(order.getPayStatus())) {
                todayAmount = todayAmount.add(order.getPayAmount() != null ? order.getPayAmount() : BigDecimal.ZERO);
                successCount++;
            }
        }

        long totalCount = todayOrders.size();
        BigDecimal successRate = totalCount > 0
                ? BigDecimal.valueOf(successCount).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Long merchantCount = 0L;
        if (StrUtil.isNotBlank(merchantNo)) {
            merchantCount = 1L;
        } else {
            try {
                if (merchantInfoService != null) {
                    merchantCount = merchantInfoService.count();
                }
            } catch (Exception e) {
                log.warn("获取商户数量失败", e);
                merchantCount = 0L;
            }
        }

        return OverviewStatsVO.builder()
                .todayAmount(todayAmount)
                .todayOrderCount(totalCount)
                .successRate(successRate)
                .merchantCount(merchantCount)
                .build();
    }

    @Override
    public List<SuccessRateTrendVO> getSuccessRateTrend(String merchantNo, Integer days) {
        int dayCount = (days == null || days <= 0) ? 7 : days;
        List<SuccessRateTrendVO> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = dayCount - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(PayOrder::getCreatedAt, dayStart)
                    .le(PayOrder::getCreatedAt, dayEnd);
            if (StrUtil.isNotBlank(merchantNo)) {
                wrapper.eq(PayOrder::getMerchantNo, merchantNo);
            }
            List<PayOrder> dayOrders = payOrderMapper.selectList(wrapper);

            long totalCount = dayOrders.size();
            long successCount = dayOrders.stream()
                    .filter(o -> PayStatusEnum.SUCCESS.getCode().equals(o.getPayStatus()))
                    .count();
            BigDecimal successRate = totalCount > 0
                    ? BigDecimal.valueOf(successCount).multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            result.add(SuccessRateTrendVO.builder()
                    .date(date.format(formatter))
                    .totalCount(totalCount)
                    .successCount(successCount)
                    .successRate(successRate)
                    .build());
        }

        return result;
    }

    @Override
    public List<ChannelDistributionVO> getPayChannelDistribution(String merchantNo) {
        LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PayOrder::getPayStatus, PayStatusEnum.SUCCESS.getCode());
        if (StrUtil.isNotBlank(merchantNo)) {
            wrapper.eq(PayOrder::getMerchantNo, merchantNo);
        }
        List<PayOrder> successOrders = payOrderMapper.selectList(wrapper);

        Map<String, List<PayOrder>> channelMap = successOrders.stream()
                .filter(o -> StrUtil.isNotBlank(o.getPayChannel()))
                .collect(Collectors.groupingBy(PayOrder::getPayChannel));

        BigDecimal totalAmount = successOrders.stream()
                .map(o -> o.getPayAmount() != null ? o.getPayAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ChannelDistributionVO> result = new ArrayList<>();
        for (Map.Entry<String, List<PayOrder>> entry : channelMap.entrySet()) {
            String channelCode = entry.getKey();
            List<PayOrder> orders = entry.getValue();
            long orderCount = orders.size();
            BigDecimal amount = orders.stream()
                    .map(o -> o.getPayAmount() != null ? o.getPayAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal percentage = totalAmount.compareTo(BigDecimal.ZERO) > 0
                    ? amount.multiply(BigDecimal.valueOf(100))
                            .divide(totalAmount, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            result.add(ChannelDistributionVO.builder()
                    .channelCode(channelCode)
                    .channelName(channelCode)
                    .orderCount(orderCount)
                    .amount(amount)
                    .percentage(percentage)
                    .build());
        }

        result.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return result;
    }

    @Override
    public List<PayOrder> getRecentOrders(String merchantNo, Integer limit) {
        int queryLimit = (limit == null || limit <= 0) ? 10 : limit;

        LambdaQueryWrapper<PayOrder> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(merchantNo)) {
            wrapper.eq(PayOrder::getMerchantNo, merchantNo);
        }
        wrapper.orderByDesc(PayOrder::getCreatedAt)
                .last("LIMIT " + queryLimit);

        return payOrderMapper.selectList(wrapper);
    }
}
