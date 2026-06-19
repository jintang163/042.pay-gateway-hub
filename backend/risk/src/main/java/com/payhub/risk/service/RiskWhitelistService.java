package com.payhub.risk.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.risk.dto.RiskListSaveRequest;
import com.payhub.risk.dto.RiskListVO;
import com.payhub.risk.entity.RiskWhitelist;

import java.util.Map;

public interface RiskWhitelistService extends IService<RiskWhitelist> {

    IPage<RiskListVO> listPage(Long current, Long size, Map<String, Object> params);

    void addToWhitelist(RiskListSaveRequest request);

    void removeFromWhitelist(Long id);

    boolean checkInList(String listType, String listValue);

    boolean checkInList(String merchantNo, String listType, String listValue);

    RiskWhitelist getByTypeAndValue(String listType, String listValue);

    RiskWhitelist getByTypeAndValue(String merchantNo, String listType, String listValue);

    RiskWhitelist isWhitelisted(String listType, String listValue);

    RiskWhitelist isWhitelisted(String merchantNo, String listType, String listValue);

    boolean checkWhitelistBypass(String listType, String listValue, String ruleCode);

    boolean checkWhitelistBypass(String merchantNo, String listType, String listValue, String ruleCode);
}
