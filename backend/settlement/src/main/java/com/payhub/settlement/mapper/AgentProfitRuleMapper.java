package com.payhub.settlement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.settlement.entity.AgentProfitRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AgentProfitRuleMapper extends BaseMapper<AgentProfitRule> {

    IPage<AgentProfitRule> selectPageList(IPage<AgentProfitRule> page, @Param("params") Map<String, Object> params);

    List<AgentProfitRule> selectByMerchantNo(@Param("merchantNo") String merchantNo);

    AgentProfitRule selectByRuleNo(@Param("ruleNo") String ruleNo);
}
