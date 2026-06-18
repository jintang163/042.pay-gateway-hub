package com.payhub.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.settlement.dto.AgentProfitRecordVO;
import com.payhub.settlement.entity.AgentProfitRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AgentProfitService extends IService<AgentProfitRecord> {

    AgentProfitRecordVO getProfitRecordById(Long id);

    AgentProfitRecordVO getProfitRecordByProfitNo(String profitNo);

    IPage<AgentProfitRecordVO> listPage(Long current, Long size, Map<String, Object> params);

    List<AgentProfitRecordVO> listByAgentMerchantNo(String agentMerchantNo);

    BigDecimal getTotalProfit(String agentMerchantNo, Integer profitStatus);

    void calculateProfit(String orderNo, String merchantNo, BigDecimal orderAmount, BigDecimal feeAmount);

    void settleAgentProfit(String settleDate);
}
