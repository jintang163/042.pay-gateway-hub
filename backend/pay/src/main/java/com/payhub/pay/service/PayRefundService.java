package com.payhub.pay.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.pay.dto.RefundRequest;
import com.payhub.pay.dto.RefundResponse;
import com.payhub.pay.entity.PayRefund;

import java.util.Map;

public interface PayRefundService extends IService<PayRefund> {

    RefundResponse applyRefund(RefundRequest request);

    RefundResponse queryRefund(String refundNo);

    IPage<PayRefund> listPage(Long current, Long size, String merchantNo, Map<String, Object> params);
}
