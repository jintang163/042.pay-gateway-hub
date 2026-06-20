package com.payhub.settlement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.settlement.entity.PaySplitDetail;

import java.util.List;
import java.util.Map;

public interface SplitTransferService {

    boolean executeTransfer(PaySplitDetail detail);

    boolean executeTransferBatch(List<PaySplitDetail> details);

    boolean retryTransfer(Long splitDetailId);

    IPage<PaySplitDetail> listPendingTransfers(Long current, Long size, Map<String, Object> params);

    List<PaySplitDetail> loadPendingTransferList(int limit);

    boolean processPendingTransfers(int limit);
}
