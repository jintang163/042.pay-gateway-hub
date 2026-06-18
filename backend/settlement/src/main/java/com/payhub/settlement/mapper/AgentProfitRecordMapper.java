package com.payhub.settlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.settlement.entity.AgentProfitRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface AgentProfitRecordMapper extends BaseMapper<AgentProfitRecord> {

    IPage<AgentProfitRecord> selectPageList(IPage<AgentProfitRecord> page, @Param("params") Map<String, Object> params);

    List<AgentProfitRecord> selectByAgentMerchantNo(@Param("agentMerchantNo") String agentMerchantNo);

    BigDecimal selectTotalProfitByAgent(@Param("agentMerchantNo") String agentMerchantNo, @Param("profitStatus") Integer profitStatus);

    List<AgentProfitRecord> selectBySettleDate(@Param("settleDate") String settleDate);
}
