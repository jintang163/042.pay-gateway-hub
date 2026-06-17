package com.payhub.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.pay.entity.PayOrder;
import com.payhub.settlement.entity.PaySplitDetail;
import com.payhub.settlement.entity.PaySplitRule;

import java.util.List;
import java.util.Map;

public interface SplitEngineService {

    List<PaySplitDetail> calculateSplit(PayOrder order, PaySplitRule rule);

    List<PaySplitDetail> executeSplit(PayOrder order);

    List<PaySplitDetail> calculateBatchSplit(Long settlementId, String settlementNo, List<PayOrder> orders);

    List<PaySplitDetail> getSplitDetailsByOrderNo(String orderNo);

    List<PaySplitDetail> getSplitDetailsBySettlementId(Long settlementId);

    IPage<PaySplitDetail> listSplitDetails(Long current, Long size, Map<String, Object> params);

    boolean updateStatusBySettlementId(Long settlementId, Integer status);
}
