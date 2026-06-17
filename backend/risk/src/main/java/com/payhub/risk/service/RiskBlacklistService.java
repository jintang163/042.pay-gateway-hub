package com.payhub.risk.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.risk.dto.RiskListSaveRequest;
import com.payhub.risk.dto.RiskListVO;
import com.payhub.risk.entity.RiskBlacklist;

import java.util.Map;

public interface RiskBlacklistService extends IService<RiskBlacklist> {

    IPage<RiskListVO> listPage(Long current, Long size, Map<String, Object> params);

    void addToBlacklist(RiskListSaveRequest request);

    void removeFromBlacklist(Long id);

    boolean checkInList(String listType, String listValue);

    RiskBlacklist getByTypeAndValue(String listType, String listValue);

    void incrementHitCount(RiskBlacklist blacklist);
}
