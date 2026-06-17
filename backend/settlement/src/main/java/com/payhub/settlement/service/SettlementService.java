package com.payhub.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.settlement.dto.SettlementVO;
import com.payhub.settlement.entity.SettlementRecord;

import java.time.LocalDate;
import java.util.Map;

public interface SettlementService extends IService<SettlementRecord> {

    SettlementVO getBySettlementNo(String settlementNo);

    IPage<SettlementVO> listPage(Long current, Long size, Map<String, Object> params);

    void generateSettlement(LocalDate settleDate);

    void confirmSettlement(Long id);
}
