package com.payhub.pay.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.pay.dto.OrderQueryRequest;
import com.payhub.pay.dto.OrderQueryResponse;
import com.payhub.pay.dto.UnifiedOrderRequest;
import com.payhub.pay.dto.UnifiedOrderResponse;
import com.payhub.pay.entity.PayOrder;

import java.util.Map;

public interface PayOrderService extends IService<PayOrder> {

    UnifiedOrderResponse unifiedOrder(UnifiedOrderRequest request);

    OrderQueryResponse queryOrder(OrderQueryRequest request);

    PayOrder getOrderDetail(String orderNo, String merchantNo);

    IPage<PayOrder> listPage(Long current, Long size, String merchantNo, Map<String, Object> params);

    String handleNotify(String channel, Map<String, String> params, String body);
}
