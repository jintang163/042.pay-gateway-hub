package com.payhub.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.settlement.dto.SplitRuleSaveRequest;
import com.payhub.settlement.dto.SplitRuleVO;
import com.payhub.settlement.entity.PaySplitRule;

import java.util.List;
import java.util.Map;

public interface SplitRuleService extends IService<PaySplitRule> {

    void saveRule(SplitRuleSaveRequest request);

    void deleteRule(Long id);

    SplitRuleVO getRuleById(Long id);

    List<SplitRuleVO> listByMerchantNo(String merchantNo);

    IPage<SplitRuleVO> listPage(Long current, Long size, Map<String, Object> params);

    void toggleRule(Long id);
}
