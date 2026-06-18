package com.payhub.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.settlement.dto.AgentProfitRuleSaveRequest;
import com.payhub.settlement.dto.AgentProfitRuleVO;
import com.payhub.settlement.entity.AgentProfitRule;

import java.util.List;
import java.util.Map;

public interface AgentProfitRuleService extends IService<AgentProfitRule> {

    void saveRule(AgentProfitRuleSaveRequest request);

    void deleteRule(Long id);

    AgentProfitRuleVO getRuleById(Long id);

    AgentProfitRuleVO getRuleByRuleNo(String ruleNo);

    List<AgentProfitRuleVO> listByMerchantNo(String merchantNo);

    IPage<AgentProfitRuleVO> listPage(Long current, Long size, Map<String, Object> params);

    void toggleRule(Long id);
}
