package com.payhub.pay.service;

import com.payhub.pay.dto.vo.OrderAttributionVO;

public interface OrderAttributionService {

    OrderAttributionVO analyzeFailReason(String orderNo, String merchantNo);
}
