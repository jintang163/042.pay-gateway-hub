package com.payhub.pay.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.enums.PayStatusEnum;
import com.payhub.common.enums.RefundStatusEnum;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.pay.dto.RefundRequest;
import com.payhub.pay.dto.RefundResponse;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.entity.PayRefund;
import com.payhub.pay.mapper.PayRefundMapper;
import com.payhub.pay.service.PayOrderService;
import com.payhub.pay.service.PayRefundService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
public class PayRefundServiceImpl extends ServiceImpl<PayRefundMapper, PayRefund> implements PayRefundService {

    @Autowired
    private PayOrderService payOrderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundResponse applyRefund(RefundRequest request) {
        LambdaQueryWrapper<PayOrder> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(PayOrder::getOrderNo, request.getOrderNo())
                .eq(PayOrder::getMerchantNo, request.getMerchantNo())
                .last("LIMIT 1");
        PayOrder order = payOrderService.getOne(orderWrapper);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "支付订单不存在");
        }
        if (!PayStatusEnum.SUCCESS.getCode().equals(order.getPayStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单状态不允许退款");
        }

        LambdaQueryWrapper<PayRefund> refundWrapper = new LambdaQueryWrapper<>();
        refundWrapper.eq(PayRefund::getMerchantNo, request.getMerchantNo())
                .eq(PayRefund::getMerchantRefundNo, request.getMerchantRefundNo())
                .last("LIMIT 1");
        PayRefund existRefund = this.getOne(refundWrapper);
        if (existRefund != null) {
            return RefundResponse.builder()
                    .refundNo(existRefund.getRefundNo())
                    .refundStatus(existRefund.getRefundStatus())
                    .build();
        }

        LambdaQueryWrapper<PayRefund> sumWrapper = new LambdaQueryWrapper<>();
        sumWrapper.eq(PayRefund::getOrderNo, request.getOrderNo())
                .eq(PayRefund::getRefundStatus, RefundStatusEnum.SUCCESS.getCode());
        BigDecimal totalRefunded = this.list(sumWrapper).stream()
                .map(PayRefund::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableRefund = order.getPayAmount().subtract(totalRefunded);
        if (request.getRefundAmount().compareTo(availableRefund) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "退款金额超过可退金额");
        }

        String refundNo = OrderNoGenerator.generateRefund();

        PayRefund refund = new PayRefund();
        refund.setRefundNo(refundNo);
        refund.setOrderNo(request.getOrderNo());
        refund.setMerchantNo(request.getMerchantNo());
        refund.setMerchantRefundNo(request.getMerchantRefundNo());
        refund.setPayAmount(order.getPayAmount());
        refund.setRefundAmount(request.getRefundAmount());
        refund.setRefundReason(request.getRefundReason());
        refund.setRefundStatus(RefundStatusEnum.PROCESSING.getCode());

        this.save(refund);

        log.info("退款申请提交成功: refundNo={}, orderNo={}, amount={}", refundNo, request.getOrderNo(), request.getRefundAmount());

        return RefundResponse.builder()
                .refundNo(refundNo)
                .refundStatus(RefundStatusEnum.PROCESSING.getCode())
                .build();
    }

    @Override
    public RefundResponse queryRefund(String refundNo) {
        LambdaQueryWrapper<PayRefund> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PayRefund::getRefundNo, refundNo)
                .last("LIMIT 1");
        PayRefund refund = this.getOne(wrapper);
        if (refund == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "退款订单不存在");
        }

        return RefundResponse.builder()
                .refundNo(refund.getRefundNo())
                .refundStatus(refund.getRefundStatus())
                .build();
    }

    @Override
    public IPage<PayRefund> listPage(Long current, Long size, String merchantNo, Map<String, Object> params) {
        Page<PayRefund> page = new Page<>(current, size);
        LambdaQueryWrapper<PayRefund> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PayRefund::getMerchantNo, merchantNo);
        if (params != null) {
            if (params.get("refundNo") != null && StrUtil.isNotBlank(params.get("refundNo").toString())) {
                wrapper.eq(PayRefund::getRefundNo, params.get("refundNo").toString());
            }
            if (params.get("orderNo") != null && StrUtil.isNotBlank(params.get("orderNo").toString())) {
                wrapper.eq(PayRefund::getOrderNo, params.get("orderNo").toString());
            }
            if (params.get("merchantRefundNo") != null && StrUtil.isNotBlank(params.get("merchantRefundNo").toString())) {
                wrapper.eq(PayRefund::getMerchantRefundNo, params.get("merchantRefundNo").toString());
            }
            if (params.get("refundStatus") != null) {
                wrapper.eq(PayRefund::getRefundStatus, params.get("refundStatus"));
            }
            if (params.get("startTime") != null && StrUtil.isNotBlank(params.get("startTime").toString())) {
                wrapper.ge(PayRefund::getCreatedAt, params.get("startTime").toString());
            }
            if (params.get("endTime") != null && StrUtil.isNotBlank(params.get("endTime").toString())) {
                wrapper.le(PayRefund::getCreatedAt, params.get("endTime").toString());
            }
        }
        wrapper.orderByDesc(PayRefund::getCreatedAt);
        return this.page(page, wrapper);
    }
}
