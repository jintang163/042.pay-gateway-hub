package com.payhub.pay.service;

import com.payhub.pay.entity.MerchantPayConfig;

import java.math.BigDecimal;

public interface PayRouterService {

    MerchantPayConfig selectChannel(String merchantNo, String payChannel, String payType, BigDecimal amount, String clientIp);
}
