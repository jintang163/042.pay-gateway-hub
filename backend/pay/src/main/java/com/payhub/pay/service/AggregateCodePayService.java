package com.payhub.pay.service;

import com.payhub.pay.dto.AggregateCodeOrderRequest;
import com.payhub.pay.dto.AggregateCodeOrderResponse;
import com.payhub.pay.dto.AggregateCodeQueryResponse;

import javax.servlet.http.HttpServletRequest;

public interface AggregateCodePayService {

    AggregateCodeOrderResponse createOrder(AggregateCodeOrderRequest request);

    AggregateCodeQueryResponse queryOrder(String orderNo, String merchantNo);

    String recognizeChannel(HttpServletRequest request);

    AggregateCodeOrderResponse getOrCreateChannelOrder(String orderNo, String channel, String payType, HttpServletRequest request);

    String getQrCodeUrl(String orderNo);

    String generateQrCodeBase64(String orderNo, int size);
}
