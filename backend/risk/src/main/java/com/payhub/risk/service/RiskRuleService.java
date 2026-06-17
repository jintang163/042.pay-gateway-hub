package com.payhub.risk.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.risk.entity.RiskRule;

import java.util.Map;

public interface RiskRuleService extends IService<RiskRule> {

    void save(RiskRule rule);

    void update(RiskRule rule);

    void deleteById(Long id);

    RiskRule getById(Long id);

    IPage<RiskRule> listPage(Long current, Long size, Map<String, Object> params);

    void enableRule(Long id);

    void disableRule(Long id);

    void reloadAllRules();

    String generateRuleContent(Long id);
}
