package com.payhub.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.channel.strategy.PayChannelStrategy;
import com.payhub.channel.strategy.PayChannelStrategyFactory;
import com.payhub.common.enums.PayChannelEnum;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.pay.entity.PayOrder;
import com.payhub.pay.mapper.PayOrderMapper;
import com.payhub.pay.service.PayOrderService;
import com.payhub.pay.service.PayRefundService;
import com.payhub.settlement.dto.ErrorOrderApplyRequest;
import com.payhub.settlement.dto.ErrorOrderAuditRequest;
import com.payhub.settlement.dto.ErrorOrderVO;
import com.payhub.settlement.entity.ErrorOrder;
import com.payhub.settlement.entity.ReconcileDetail;
import com.payhub.settlement.enums.*;
import com.payhub.settlement.mapper.ErrorOrderMapper;
import com.payhub.settlement.mapper.ReconcileDetailMapper;
import com.payhub.settlement.service.ErrorOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class ErrorOrderServiceImpl extends ServiceImpl<ErrorOrderMapper, ErrorOrder> implements ErrorOrderService {

    @Autowired
    private ReconcileDetailMapper reconcileDetailMapper;

    @Autowired
    private PayOrderMapper payOrderMapper;

    @Autowired
    private PayOrderService payOrderService;

    @Autowired
    private PayRefundService payRefundService;

    @Autowired
    private PayChannelStrategyFactory payChannelStrategyFactory;

    @Override
    public IPage<ErrorOrderVO> listPage(Long current, Long size, Map<String, Object> params) {
        Page<ErrorOrder> page = new Page<>(current, size);
        LambdaQueryWrapper<ErrorOrder> wrapper = buildQueryWrapper(params);
        wrapper.orderByDesc(ErrorOrder::getCreatedAt, ErrorOrder::getId);
        IPage<ErrorOrder> recordPage = this.page(page, wrapper);
        return recordPage.convert(this::convertToVO);
    }

    @Override
    public ErrorOrderVO getByErrorNo(String errorNo) {
        if (StrUtil.isBlank(errorNo)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "差错单号不能为空");
        }
        LambdaQueryWrapper<ErrorOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErrorOrder::getErrorNo, errorNo);
        ErrorOrder errorOrder = this.getOne(wrapper);
        if (errorOrder == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "差错单不存在");
        }
        return convertToVO(errorOrder);
    }

    @Override
    public ErrorOrderVO getById(Long id) {
        ErrorOrder errorOrder = super.getById(id);
        if (errorOrder == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "差错单不存在");
        }
        return convertToVO(errorOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErrorOrderVO applyErrorOrder(ErrorOrderApplyRequest request, String applyUserId, String applyUserName) {
        if (request == null || request.getReconcileDetailId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "差异明细ID不能为空");
        }
        if (request.getHandleType() == null || ErrorHandleTypeEnum.getByCode(request.getHandleType()) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无效的处理方式");
        }

        ReconcileDetail detail = reconcileDetailMapper.selectById(request.getReconcileDetailId());
        if (detail == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "差异明细不存在");
        }

        if (StrUtil.isNotBlank(detail.getErrorOrderNo())) {
            throw new BusinessException(ResultCode.FAIL, "该差异已生成差错单，请勿重复申请");
        }

        ErrorOrder errorOrder = new ErrorOrder();
        errorOrder.setErrorNo(OrderNoGenerator.generateWithPrefix("ERR"));
        errorOrder.setReconcileNo(detail.getReconcileNo());
        errorOrder.setReconcileDetailId(detail.getId());
        errorOrder.setPayChannel(detail.getPayChannel());
        errorOrder.setErrorType(detail.getDiffType());
        errorOrder.setHandleType(request.getHandleType());
        errorOrder.setOrderNo(detail.getOrderNo());
        errorOrder.setMerchantNo(detail.getMerchantNo());
        errorOrder.setChannelTradeNo(detail.getChannelTradeNo());
        errorOrder.setOrderAmount(detail.getLocalAmount());
        errorOrder.setActualAmount(detail.getChannelAmount());
        errorOrder.setDiffAmount(detail.getDiffAmount());
        errorOrder.setErrorStatus(ErrorStatusEnum.PENDING.getCode());
        errorOrder.setApplyUserId(applyUserId);
        errorOrder.setApplyUserName(applyUserName);
        errorOrder.setApplyTime(LocalDateTime.now());
        errorOrder.setApplyRemark(request.getApplyRemark());
        errorOrder.setAuditStatus(AuditStatusEnum.PENDING.getCode());
        this.save(errorOrder);

        detail.setErrorOrderNo(errorOrder.getErrorNo());
        detail.setHandleStatus(ReconcileHandleStatusEnum.PROCESSING.getCode());
        reconcileDetailMapper.updateById(detail);

        log.info("申请差错单成功, errorNo:{}, detailId:{}", errorOrder.getErrorNo(), request.getReconcileDetailId());
        return convertToVO(errorOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErrorOrderVO auditErrorOrder(ErrorOrderAuditRequest request, String auditUserId, String auditUserName) {
        if (request == null || request.getErrorOrderId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "差错单ID不能为空");
        }
        if (request.getAuditStatus() == null || AuditStatusEnum.getByCode(request.getAuditStatus()) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "无效的审核状态");
        }

        ErrorOrder errorOrder = this.getById(request.getErrorOrderId());
        if (errorOrder == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "差错单不存在");
        }
        if (!AuditStatusEnum.PENDING.getCode().equals(errorOrder.getAuditStatus())) {
            throw new BusinessException(ResultCode.FAIL, "该差错单已审核，请勿重复操作");
        }

        errorOrder.setAuditStatus(request.getAuditStatus());
        errorOrder.setAuditUserId(auditUserId);
        errorOrder.setAuditUserName(auditUserName);
        errorOrder.setAuditTime(LocalDateTime.now());
        errorOrder.setAuditRemark(request.getAuditRemark());

        if (AuditStatusEnum.REJECTED.getCode().equals(request.getAuditStatus())) {
            errorOrder.setErrorStatus(ErrorStatusEnum.CLOSED.getCode());

            if (errorOrder.getReconcileDetailId() != null) {
                ReconcileDetail detail = reconcileDetailMapper.selectById(errorOrder.getReconcileDetailId());
                if (detail != null) {
                    detail.setErrorOrderNo(null);
                    detail.setHandleStatus(ReconcileHandleStatusEnum.PENDING.getCode());
                    reconcileDetailMapper.updateById(detail);
                }
            }
        } else if (AuditStatusEnum.APPROVED.getCode().equals(request.getAuditStatus())) {
            errorOrder.setErrorStatus(ErrorStatusEnum.PROCESSING.getCode());
        }

        this.updateById(errorOrder);
        log.info("审核差错单完成, errorNo:{}, auditStatus:{}", errorOrder.getErrorNo(), request.getAuditStatus());
        return convertToVO(errorOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErrorOrderVO processSupplementOrder(Long errorOrderId, String handleUserId, String handleUserName) {
        ErrorOrder errorOrder = validateAndGetErrorOrder(errorOrderId);
        log.info("开始执行补单处理, errorNo:{}", errorOrder.getErrorNo());

        try {
            ReconcileDetail detail = null;
            if (errorOrder.getReconcileDetailId() != null) {
                detail = reconcileDetailMapper.selectById(errorOrder.getReconcileDetailId());
            }

            if (detail == null || StrUtil.isBlank(detail.getChannelTradeNo())) {
                throw new BusinessException(ResultCode.FAIL, "缺少渠道交易号信息，无法补单");
            }

            PayChannelStrategy strategy = payChannelStrategyFactory.getStrategy(errorOrder.getPayChannel());
            com.payhub.channel.dto.QueryOrderResponse queryResp = strategy.queryOrder(null, detail.getChannelTradeNo());

            if (queryResp == null || !queryResp.isSuccess()) {
                throw new BusinessException(ResultCode.FAIL, "渠道订单查询失败: " + (queryResp != null ? queryResp.getMessage() : "未知错误"));
            }

            PayOrder newOrder = new PayOrder();
            String newOrderNo = OrderNoGenerator.generateWithPrefix("PAY");
            newOrder.setOrderNo(newOrderNo);
            newOrder.setMerchantNo(StrUtil.isNotBlank(errorOrder.getMerchantNo()) ? errorOrder.getMerchantNo() : "UNKNOWN");
            newOrder.setMerchantOrderNo("SUP_" + System.currentTimeMillis());
            newOrder.setPayAmount(errorOrder.getActualAmount() != null ? errorOrder.getActualAmount() : BigDecimal.ZERO);
            newOrder.setActualAmount(errorOrder.getActualAmount() != null ? errorOrder.getActualAmount() : BigDecimal.ZERO);
            newOrder.setPayChannel(errorOrder.getPayChannel());
            newOrder.setPayStatus(com.payhub.common.enums.PayStatusEnum.SUCCESS.getCode());
            newOrder.setChannelTradeNo(detail.getChannelTradeNo());
            newOrder.setPayTime(queryResp.getPayTime() != null ? queryResp.getPayTime() : LocalDateTime.now());
            newOrder.setProductSubject("差错补单-" + errorOrder.getErrorNo());
            newOrder.setPayType("SUPPLEMENT");
            payOrderMapper.insert(newOrder);

            errorOrder.setNewOrderNo(newOrderNo);
            errorOrder.setErrorStatus(ErrorStatusEnum.SUCCESS.getCode());
            errorOrder.setHandleUserId(handleUserId);
            errorOrder.setHandleUserName(handleUserName);
            errorOrder.setHandleTime(LocalDateTime.now());
            errorOrder.setHandleResult("补单成功，新订单号: " + newOrderNo);
            this.updateById(errorOrder);

            updateDetailHandleStatus(errorOrder.getReconcileDetailId(), ReconcileHandleStatusEnum.PROCESSED, "补单成功", handleUserId, handleUserName);

            log.info("补单处理成功, errorNo:{}, newOrderNo:{}", errorOrder.getErrorNo(), newOrderNo);
        } catch (Exception e) {
            log.error("补单处理失败, errorNo:{}", errorOrder.getErrorNo(), e);
            errorOrder.setErrorStatus(ErrorStatusEnum.FAIL.getCode());
            errorOrder.setHandleUserId(handleUserId);
            errorOrder.setHandleUserName(handleUserName);
            errorOrder.setHandleTime(LocalDateTime.now());
            errorOrder.setHandleResult("补单失败: " + e.getMessage());
            this.updateById(errorOrder);
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException(ResultCode.FAIL, "补单失败: " + e.getMessage());
        }

        return convertToVO(errorOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErrorOrderVO processRefund(Long errorOrderId, String handleUserId, String handleUserName) {
        ErrorOrder errorOrder = validateAndGetErrorOrder(errorOrderId);
        log.info("开始执行退款处理, errorNo:{}", errorOrder.getErrorNo());

        try {
            if (StrUtil.isBlank(errorOrder.getOrderNo())) {
                throw new BusinessException(ResultCode.FAIL, "缺少平台订单号，无法退款");
            }

            PayOrder payOrder = payOrderMapper.selectOne(
                    new LambdaQueryWrapper<PayOrder>().eq(PayOrder::getOrderNo, errorOrder.getOrderNo()));
            if (payOrder == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "平台订单不存在");
            }
            if (StrUtil.isBlank(payOrder.getMerchantNo())) {
                throw new BusinessException(ResultCode.FAIL, "订单缺少商户号，无法退款");
            }

            BigDecimal refundAmount = errorOrder.getDiffAmount() != null && errorOrder.getDiffAmount().compareTo(BigDecimal.ZERO) > 0
                    ? errorOrder.getDiffAmount().abs() : payOrder.getActualAmount();

            com.payhub.pay.dto.RefundRequest refundRequest = com.payhub.pay.dto.RefundRequest.builder()
                    .orderNo(payOrder.getOrderNo())
                    .merchantRefundNo("ERR_REFUND_" + errorOrder.getErrorNo())
                    .refundAmount(refundAmount)
                    .refundReason("差错退款-" + errorOrder.getErrorNo())
                    .build();

            com.payhub.pay.dto.RefundResponse refundResponse = payRefundService.applyRefund(refundRequest, null);

            if (refundResponse == null) {
                throw new BusinessException(ResultCode.FAIL, "退款服务返回空结果");
            }

            String refundNo = refundResponse.getRefundNo();
            String channelRefundNo = refundResponse.getChannelRefundNo();
            Integer refundStatus = refundResponse.getRefundStatus();

            errorOrder.setRefundNo(refundNo);

            if (com.payhub.common.enums.RefundStatusEnum.SUCCESS.getCode().equals(refundStatus)) {
                errorOrder.setErrorStatus(ErrorStatusEnum.SUCCESS.getCode());
                errorOrder.setHandleResult("退款成功, 退款单号: " + refundNo + ", 通道退款号: " + channelRefundNo);
                updateDetailHandleStatus(errorOrder.getReconcileDetailId(), ReconcileHandleStatusEnum.PROCESSED, "退款成功", handleUserId, handleUserName);
                log.info("差错退款成功, errorNo:{}, refundNo:{}, channelRefundNo:{}", errorOrder.getErrorNo(), refundNo, channelRefundNo);
            } else if (com.payhub.common.enums.RefundStatusEnum.PROCESSING.getCode().equals(refundStatus)) {
                errorOrder.setErrorStatus(ErrorStatusEnum.PROCESSING.getCode());
                errorOrder.setHandleResult("退款处理中, 退款单号: " + refundNo + ", 通道退款号: " + (StrUtil.isNotBlank(channelRefundNo) ? channelRefundNo : "待返回"));
                updateDetailHandleStatus(errorOrder.getReconcileDetailId(), ReconcileHandleStatusEnum.PROCESSING, "退款处理中", handleUserId, handleUserName);
                log.info("差错退款处理中, errorNo:{}, refundNo:{}", errorOrder.getErrorNo(), refundNo);
            } else {
                errorOrder.setErrorStatus(ErrorStatusEnum.FAIL.getCode());
                errorOrder.setHandleResult("退款失败, 退款单号: " + refundNo);
                updateDetailHandleStatus(errorOrder.getReconcileDetailId(), ReconcileHandleStatusEnum.PENDING, "退款失败", handleUserId, handleUserName);
                log.warn("差错退款失败, errorNo:{}, refundNo:{}, refundStatus:{}", errorOrder.getErrorNo(), refundNo, refundStatus);
            }

            errorOrder.setHandleUserId(handleUserId);
            errorOrder.setHandleUserName(handleUserName);
            errorOrder.setHandleTime(LocalDateTime.now());
            this.updateById(errorOrder);

        } catch (Exception e) {
            log.error("退款处理失败, errorNo:{}", errorOrder.getErrorNo(), e);
            errorOrder.setErrorStatus(ErrorStatusEnum.FAIL.getCode());
            errorOrder.setHandleUserId(handleUserId);
            errorOrder.setHandleUserName(handleUserName);
            errorOrder.setHandleTime(LocalDateTime.now());
            errorOrder.setHandleResult("退款失败: " + e.getMessage());
            this.updateById(errorOrder);
            if (e instanceof BusinessException) {
                throw e;
            }
            throw new BusinessException(ResultCode.FAIL, "退款失败: " + e.getMessage());
        }

        return convertToVO(errorOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErrorOrderVO processAdjust(Long errorOrderId, String handleUserId, String handleUserName) {
        ErrorOrder errorOrder = validateAndGetErrorOrder(errorOrderId);
        log.info("开始执行调账处理, errorNo:{}", errorOrder.getErrorNo());

        try {
            errorOrder.setErrorStatus(ErrorStatusEnum.SUCCESS.getCode());
            errorOrder.setHandleUserId(handleUserId);
            errorOrder.setHandleUserName(handleUserName);
            errorOrder.setHandleTime(LocalDateTime.now());
            errorOrder.setHandleResult("调账处理完成，已登记差异金额待财务结算");
            this.updateById(errorOrder);

            updateDetailHandleStatus(errorOrder.getReconcileDetailId(), ReconcileHandleStatusEnum.PROCESSED, "调账完成", handleUserId, handleUserName);

            log.info("调账处理成功, errorNo:{}", errorOrder.getErrorNo());
        } catch (Exception e) {
            log.error("调账处理失败, errorNo:{}", errorOrder.getErrorNo(), e);
            errorOrder.setErrorStatus(ErrorStatusEnum.FAIL.getCode());
            errorOrder.setHandleResult("调账失败: " + e.getMessage());
            this.updateById(errorOrder);
            throw new BusinessException(ResultCode.FAIL, "调账失败: " + e.getMessage());
        }

        return convertToVO(errorOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErrorOrderVO processIgnore(Long errorOrderId, String handleUserId, String handleUserName) {
        ErrorOrder errorOrder = validateAndGetErrorOrder(errorOrderId);
        log.info("开始执行忽略处理, errorNo:{}", errorOrder.getErrorNo());

        try {
            errorOrder.setErrorStatus(ErrorStatusEnum.CLOSED.getCode());
            errorOrder.setHandleUserId(handleUserId);
            errorOrder.setHandleUserName(handleUserName);
            errorOrder.setHandleTime(LocalDateTime.now());
            errorOrder.setHandleResult("已忽略，无需处理");
            this.updateById(errorOrder);

            updateDetailHandleStatus(errorOrder.getReconcileDetailId(), ReconcileHandleStatusEnum.IGNORED, "已忽略", handleUserId, handleUserName);

            log.info("忽略处理成功, errorNo:{}", errorOrder.getErrorNo());
        } catch (Exception e) {
            log.error("忽略处理失败, errorNo:{}", errorOrder.getErrorNo(), e);
            throw new BusinessException(ResultCode.FAIL, "处理失败: " + e.getMessage());
        }

        return convertToVO(errorOrder);
    }

    @Override
    public List<ErrorOrderVO> listByReconcileDetailId(Long reconcileDetailId) {
        if (reconcileDetailId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ErrorOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErrorOrder::getReconcileDetailId, reconcileDetailId);
        wrapper.orderByDesc(ErrorOrder::getCreatedAt);
        List<ErrorOrder> list = this.list(wrapper);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<ErrorOrderVO> result = new ArrayList<>();
        for (ErrorOrder errorOrder : list) {
            result.add(convertToVO(errorOrder));
        }
        return result;
    }

    private ErrorOrder validateAndGetErrorOrder(Long errorOrderId) {
        if (errorOrderId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "差错单ID不能为空");
        }
        ErrorOrder errorOrder = this.getById(errorOrderId);
        if (errorOrder == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "差错单不存在");
        }
        if (!AuditStatusEnum.APPROVED.getCode().equals(errorOrder.getAuditStatus())) {
            throw new BusinessException(ResultCode.FAIL, "差错单未通过审核，无法处理");
        }
        if (!ErrorStatusEnum.PROCESSING.getCode().equals(errorOrder.getErrorStatus())) {
            throw new BusinessException(ResultCode.FAIL, "差错单状态不允许处理，当前状态: " + errorOrder.getErrorStatus());
        }
        return errorOrder;
    }

    private void updateDetailHandleStatus(Long detailId, ReconcileHandleStatusEnum status, String remark, String userId, String userName) {
        if (detailId == null) {
            return;
        }
        ReconcileDetail detail = reconcileDetailMapper.selectById(detailId);
        if (detail != null) {
            detail.setHandleStatus(status.getCode());
            detail.setHandleRemark(remark);
            detail.setHandleUserId(userId);
            detail.setHandleUserName(userName);
            detail.setHandleTime(LocalDateTime.now());
            reconcileDetailMapper.updateById(detail);
        }
    }

    private LambdaQueryWrapper<ErrorOrder> buildQueryWrapper(Map<String, Object> params) {
        LambdaQueryWrapper<ErrorOrder> wrapper = new LambdaQueryWrapper<>();
        if (params != null) {
            if (params.get("errorNo") != null && StrUtil.isNotBlank(params.get("errorNo").toString())) {
                wrapper.eq(ErrorOrder::getErrorNo, params.get("errorNo"));
            }
            if (params.get("reconcileNo") != null && StrUtil.isNotBlank(params.get("reconcileNo").toString())) {
                wrapper.eq(ErrorOrder::getReconcileNo, params.get("reconcileNo"));
            }
            if (params.get("payChannel") != null && StrUtil.isNotBlank(params.get("payChannel").toString())) {
                wrapper.eq(ErrorOrder::getPayChannel, params.get("payChannel"));
            }
            if (params.get("errorType") != null) {
                wrapper.eq(ErrorOrder::getErrorType, params.get("errorType"));
            }
            if (params.get("errorStatus") != null) {
                wrapper.eq(ErrorOrder::getErrorStatus, params.get("errorStatus"));
            }
            if (params.get("auditStatus") != null) {
                wrapper.eq(ErrorOrder::getAuditStatus, params.get("auditStatus"));
            }
            if (params.get("handleType") != null) {
                wrapper.eq(ErrorOrder::getHandleType, params.get("handleType"));
            }
            if (params.get("orderNo") != null && StrUtil.isNotBlank(params.get("orderNo").toString())) {
                wrapper.eq(ErrorOrder::getOrderNo, params.get("orderNo"));
            }
            if (params.get("merchantNo") != null && StrUtil.isNotBlank(params.get("merchantNo").toString())) {
                wrapper.eq(ErrorOrder::getMerchantNo, params.get("merchantNo"));
            }
        }
        return wrapper;
    }

    private ErrorOrderVO convertToVO(ErrorOrder errorOrder) {
        ErrorOrderVO vo = BeanUtil.copyProperties(errorOrder, ErrorOrderVO.class);

        PayChannelEnum channelEnum = PayChannelEnum.getByCode(errorOrder.getPayChannel());
        if (channelEnum != null) {
            vo.setPayChannelDesc(channelEnum.getDesc());
        }

        ReconcileDiffTypeEnum diffTypeEnum = ReconcileDiffTypeEnum.getByCode(errorOrder.getErrorType());
        if (diffTypeEnum != null) {
            vo.setErrorTypeDesc(diffTypeEnum.getDesc());
        }

        if (errorOrder.getHandleType() != null) {
            ErrorHandleTypeEnum handleTypeEnum = ErrorHandleTypeEnum.getByCode(errorOrder.getHandleType());
            if (handleTypeEnum != null) {
                vo.setHandleTypeDesc(handleTypeEnum.getDesc());
            }
        }

        ErrorStatusEnum errorStatusEnum = ErrorStatusEnum.getByCode(errorOrder.getErrorStatus());
        if (errorStatusEnum != null) {
            vo.setErrorStatusDesc(errorStatusEnum.getDesc());
        }

        if (errorOrder.getAuditStatus() != null) {
            AuditStatusEnum auditStatusEnum = com.payhub.settlement.enums.AuditStatusEnum.getByCode(errorOrder.getAuditStatus());
            if (auditStatusEnum != null) {
                vo.setAuditStatusDesc(auditStatusEnum.getDesc());
            }
        }

        return vo;
    }
}
