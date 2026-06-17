package com.payhub.pay.service;

import com.payhub.pay.dto.vo.ChannelDistributionVO;
import com.payhub.pay.dto.vo.OverviewStatsVO;
import com.payhub.pay.dto.vo.SuccessRateTrendVO;
import com.payhub.pay.entity.PayOrder;

import java.util.List;

public interface DashboardService {

    OverviewStatsVO getOverviewStats(String merchantNo);

    List<SuccessRateTrendVO> getSuccessRateTrend(String merchantNo, Integer days);

    List<ChannelDistributionVO> getPayChannelDistribution(String merchantNo);

    List<PayOrder> getRecentOrders(String merchantNo, Integer limit);
}
