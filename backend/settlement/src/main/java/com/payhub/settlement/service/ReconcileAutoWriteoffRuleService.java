package com.payhub.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.settlement.dto.AutoWriteoffRuleSaveRequest;
import com.payhub.settlement.dto.AutoWriteoffRuleVO;
import com.payhub.settlement.entity.ReconcileAutoWriteoffRule;
import com.payhub.settlement.entity.ReconcileDetail;

import java.util.Map;

public interface ReconcileAutoWriteoffRuleService extends IService<ReconcileAutoWriteoffRule> {

    IPage<AutoWriteoffRuleVO> listPage(Long current, Long size, Map<String, Object> params);

    void addRule(AutoWriteoffRuleSaveRequest request);

    void updateRule(AutoWriteoffRuleSaveRequest request);

    void deleteRule(Long id);

    ReconcileAutoWriteoffRule matchRule(ReconcileDetail detail);
}
