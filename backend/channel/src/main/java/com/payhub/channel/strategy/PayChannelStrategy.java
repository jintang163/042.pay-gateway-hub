package com.payhub.channel.strategy;

import com.payhub.channel.dto.*;

import java.util.Map;

public interface PayChannelStrategy {

    UnifiedOrderResponse unifiedOrder(UnifiedOrderRequest request);

    QueryOrderResponse queryOrder(String orderNo, String channelTradeNo);

    RefundResponse refund(RefundRequest request);

    QueryRefundResponse queryRefund(String refundNo, String channelRefundNo);

    NotifyResult parseNotify(String notifyData, Map<String, String> params);

    boolean verifyNotify(Map<String, String> params);
}
