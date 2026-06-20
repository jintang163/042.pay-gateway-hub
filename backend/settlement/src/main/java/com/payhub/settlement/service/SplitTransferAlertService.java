package com.payhub.settlement.service;

import com.payhub.settlement.entity.PaySplitDetail;

import java.util.List;

public interface SplitTransferAlertService {

    void alertTransferFailed(PaySplitDetail detail);

    void alertTransferRetryExhausted(PaySplitDetail detail);

    void alertBatchTransferFailed(List<PaySplitDetail> details);
}
