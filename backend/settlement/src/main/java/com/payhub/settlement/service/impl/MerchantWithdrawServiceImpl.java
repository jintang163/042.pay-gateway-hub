package com.payhub.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.merchant.entity.MerchantInfo;
import com.payhub.merchant.service.MerchantInfoService;
import com.payhub.settlement.dto.MerchantBalanceVO;
import com.payhub.settlement.dto.MerchantWithdrawApplyRequest;
import com.payhub.settlement.dto.MerchantWithdrawAuditRequest;
import com.payhub.settlement.dto.MerchantWithdrawVO;
import com.payhub.settlement.entity.MerchantWithdraw;
import com.payhub.settlement.enums.MerchantWithdrawStatusEnum;
import com.payhub.settlement.enums.WithdrawTypeEnum;
import com.payhub.settlement.mapper.MerchantWithdrawMapper;
import com.payhub.settlement.service.MerchantWithdrawService;
import com.payhub.settlement.service.UnifiedTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MerchantWithdrawServiceImpl extends ServiceImpl<MerchantWithdrawMapper, MerchantWithdraw> implements MerchantWithdrawService {

    @Autowired
    private MerchantInfoService merchantInfoService;

    @Autowired
    private UnifiedTransferService unifiedTransferService;

    @Value("${payhub.merchant.withdraw.min-amount:10}")
    private BigDecimal minWithdrawAmount;

    @Value("${payhub.merchant.withdraw.max-amount:500000}")
    private BigDecimal maxWithdrawAmount;

    @Value("${payhub.merchant.withdraw.audit-threshold:50000}")
    private BigDecimal auditThreshold;

    @Value("${payhub.merchant.withdraw.t1-arrive-days:1}")
    private Integer t1ArriveDays;

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyWithdraw(MerchantWithdrawApplyRequest request) {
        WithdrawTypeEnum withdrawType = WithdrawTypeEnum.getByCode(request.getWithdrawType());
        if (withdrawType == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的提现类型");
        }

        BigDecimal availableBalance = getAvailableBalance(request.getMerchantNo());
        if (request.getWithdrawAmount().compareTo(minWithdrawAmount) < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "提现金额不能小于最低提现金额" + minWithdrawAmount + "元");
        }
        if (request.getWithdrawAmount().compareTo(maxWithdrawAmount) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "提现金额不能超过最高提现金额" + maxWithdrawAmount + "元");
        }
        if (request.getWithdrawAmount().compareTo(availableBalance) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "可提现余额不足");
        }

        BigDecimal feeAmount = calculateFee(request.getWithdrawAmount(), request.getWithdrawType());
        BigDecimal actualAmount = request.getWithdrawAmount().subtract(feeAmount);

        MerchantInfo merchantInfo = getMerchantInfo(request.getMerchantNo());

        MerchantWithdraw withdraw = new MerchantWithdraw();
        withdraw.setWithdrawNo(OrderNoGenerator.generateWithPrefix("MW"));
        withdraw.setMerchantNo(request.getMerchantNo());
        withdraw.setMerchantName(merchantInfo != null ? merchantInfo.getMerchantName() : "");
        withdraw.setWithdrawAmount(request.getWithdrawAmount());
        withdraw.setActualAmount(actualAmount);
        withdraw.setFeeAmount(feeAmount);
        withdraw.setWithdrawType(request.getWithdrawType());

        boolean needAudit = request.getWithdrawAmount().compareTo(auditThreshold) >= 0;
        if (needAudit) {
            withdraw.setWithdrawStatus(MerchantWithdrawStatusEnum.PENDING_AUDIT.getCode());
            log.info("大额提现需人工审核: withdrawNo={}, merchantNo={}, amount={}",
                    withdraw.getWithdrawNo(), request.getMerchantNo(), request.getWithdrawAmount());
        } else {
            withdraw.setWithdrawStatus(MerchantWithdrawStatusEnum.AUDIT_PASSED.getCode());
            withdraw.setAuditTime(LocalDateTime.now());
            withdraw.setAuditUser("SYSTEM");
            withdraw.setAuditRemark("自动审核通过");
            log.info("小额提现自动审核通过: withdrawNo={}, merchantNo={}, amount={}",
                    withdraw.getWithdrawNo(), request.getMerchantNo(), request.getWithdrawAmount());
        }

        withdraw.setBankName(request.getBankName());
        withdraw.setBankAccount(request.getBankAccount());
        withdraw.setAccountName(request.getAccountName());
        withdraw.setRemark(request.getRemark());
        this.save(withdraw);

        if (!needAudit && WithdrawTypeEnum.INSTANT.getCode().equals(request.getWithdrawType())) {
            log.info("即时到账提现，自动执行转账: withdrawNo={}", withdraw.getWithdrawNo());
            executeTransfer(withdraw.getId());
        }

        log.info("商户提现申请提交成功: withdrawNo={}, merchantNo={}, amount={}, type={}, needAudit={}",
                withdraw.getWithdrawNo(), request.getMerchantNo(), request.getWithdrawAmount(),
                withdrawType.getDesc(), needAudit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditWithdraw(MerchantWithdrawAuditRequest request) {
        MerchantWithdraw withdraw = this.getById(request.getId());
        if (withdraw == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "提现申请不存在");
        }
        if (!MerchantWithdrawStatusEnum.PENDING_AUDIT.getCode().equals(withdraw.getWithdrawStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前状态不允许审核");
        }

        if (request.getAuditStatus() == 1) {
            withdraw.setWithdrawStatus(MerchantWithdrawStatusEnum.AUDIT_PASSED.getCode());
            log.info("提现审核通过: id={}, auditUser={}", request.getId(), request.getAuditUser());

            if (WithdrawTypeEnum.INSTANT.getCode().equals(withdraw.getWithdrawType())) {
                log.info("即时到账提现，审核通过后自动执行转账: withdrawNo={}", withdraw.getWithdrawNo());
            }
        } else {
            withdraw.setWithdrawStatus(MerchantWithdrawStatusEnum.AUDIT_REJECTED.getCode());
            log.info("提现审核拒绝: id={}, auditUser={}, remark={}",
                    request.getId(), request.getAuditUser(), request.getAuditRemark());
        }
        withdraw.setAuditUser(request.getAuditUser());
        withdraw.setAuditRemark(request.getAuditRemark());
        withdraw.setAuditTime(LocalDateTime.now());
        this.updateById(withdraw);

        if (request.getAuditStatus() == 1 && WithdrawTypeEnum.INSTANT.getCode().equals(withdraw.getWithdrawType())) {
            executeTransfer(withdraw.getId());
        }
    }

    @Override
    public MerchantWithdrawVO getWithdrawById(Long id) {
        MerchantWithdraw withdraw = this.getById(id);
        if (withdraw == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "提现申请不存在");
        }
        return convertToVO(withdraw);
    }

    @Override
    public MerchantWithdrawVO getWithdrawByWithdrawNo(String withdrawNo) {
        MerchantWithdraw withdraw = baseMapper.selectByWithdrawNo(withdrawNo);
        return withdraw != null ? convertToVO(withdraw) : null;
    }

    @Override
    public IPage<MerchantWithdrawVO> listPage(Long current, Long size, Map<String, Object> params) {
        Page<MerchantWithdraw> page = new Page<>(current, size);
        IPage<MerchantWithdraw> withdrawPage = baseMapper.selectPageList(page, params);
        return withdrawPage.convert(this::convertToVO);
    }

    @Override
    public List<MerchantWithdrawVO> listByMerchantNo(String merchantNo) {
        List<MerchantWithdraw> withdraws = baseMapper.selectByMerchantNo(merchantNo);
        return withdraws.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public BigDecimal getTotalWithdraw(String merchantNo, Integer withdrawStatus) {
        BigDecimal total = baseMapper.selectTotalWithdrawByMerchant(merchantNo, withdrawStatus);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getAvailableBalance(String merchantNo) {
        BigDecimal totalSettle = baseMapper.selectTotalSettleAmount(merchantNo);
        BigDecimal totalWithdraw = getTotalWithdraw(merchantNo, null);

        BigDecimal pendingWithdraw = getTotalWithdraw(merchantNo, MerchantWithdrawStatusEnum.PENDING_AUDIT.getCode());
        BigDecimal approvedWithdraw = getTotalWithdraw(merchantNo, MerchantWithdrawStatusEnum.AUDIT_PASSED.getCode());
        BigDecimal successWithdraw = getTotalWithdraw(merchantNo, MerchantWithdrawStatusEnum.SUCCESS.getCode());
        BigDecimal transferringWithdraw = getTotalWithdraw(merchantNo, MerchantWithdrawStatusEnum.TRANSFERRING.getCode());
        BigDecimal arrivedWithdraw = getTotalWithdraw(merchantNo, MerchantWithdrawStatusEnum.ARRIVED.getCode());

        BigDecimal usedWithdraw = pendingWithdraw.add(approvedWithdraw).add(successWithdraw)
                .add(transferringWithdraw).add(arrivedWithdraw);

        return totalSettle.subtract(usedWithdraw);
    }

    @Override
    public MerchantBalanceVO getMerchantBalance(String merchantNo) {
        MerchantBalanceVO vo = new MerchantBalanceVO();
        vo.setMerchantNo(merchantNo);

        BigDecimal totalSettle = baseMapper.selectTotalSettleAmount(merchantNo);
        BigDecimal totalWithdraw = getTotalWithdraw(merchantNo, null);
        BigDecimal availableBalance = getAvailableBalance(merchantNo);
        BigDecimal pendingWithdraw = getTotalWithdraw(merchantNo, MerchantWithdrawStatusEnum.PENDING_AUDIT.getCode());

        vo.setTotalSettleAmount(totalSettle);
        vo.setTotalWithdrawAmount(totalWithdraw);
        vo.setAvailableBalance(availableBalance);
        vo.setPendingWithdrawAmount(pendingWithdraw);
        vo.setT1Balance(availableBalance);
        vo.setInstantBalance(availableBalance);
        vo.setMinWithdrawAmount(minWithdrawAmount);
        vo.setMaxWithdrawAmount(maxWithdrawAmount);
        vo.setInstantFeeRate(WithdrawTypeEnum.INSTANT.getFeeRate());
        vo.setAuditThreshold(auditThreshold);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeTransfer(Long id) {
        MerchantWithdraw withdraw = this.getById(id);
        if (withdraw == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "提现申请不存在");
        }
        if (!MerchantWithdrawStatusEnum.AUDIT_PASSED.getCode().equals(withdraw.getWithdrawStatus())
                && !MerchantWithdrawStatusEnum.TRANSFERRING.getCode().equals(withdraw.getWithdrawStatus())
                && !MerchantWithdrawStatusEnum.FAILED.getCode().equals(withdraw.getWithdrawStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前状态不允许转账");
        }

        log.info("商户提现走统一代付链路: id={}, withdrawNo={}", id, withdraw.getWithdrawNo());
        UnifiedTransferService.TransferContext ctx = unifiedTransferService.buildContextForMerchantWithdraw(id);
        unifiedTransferService.executeTransfer(ctx);
        log.info("商户提现统一代付执行完成: id={}, withdrawNo={}", id, withdraw.getWithdrawNo());

        MerchantWithdraw updated = this.getById(id);
        if (MerchantWithdrawStatusEnum.SUCCESS.getCode().equals(updated.getWithdrawStatus())) {
            if (WithdrawTypeEnum.INSTANT.getCode().equals(updated.getWithdrawType())) {
                updated.setWithdrawStatus(MerchantWithdrawStatusEnum.ARRIVED.getCode());
                updated.setArriveTime(LocalDateTime.now());
                this.updateById(updated);
                log.info("即时到账提现已标记为已到账: withdrawNo={}", updated.getWithdrawNo());
            } else if (WithdrawTypeEnum.T1.getCode().equals(updated.getWithdrawType())) {
                updated.setArriveTime(LocalDateTime.now().plusDays(t1ArriveDays));
                this.updateById(updated);
                log.info("T+1提现预计到账时间已设置: withdrawNo={}, arriveTime={}",
                        updated.getWithdrawNo(), updated.getArriveTime());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryFailedWithdraw() {
        LambdaQueryWrapper<MerchantWithdraw> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantWithdraw::getWithdrawStatus, MerchantWithdrawStatusEnum.FAILED.getCode());
        List<MerchantWithdraw> failedList = this.list(wrapper);
        for (MerchantWithdraw withdraw : failedList) {
            try {
                log.info("重试商户提现(统一链路): id={}, withdrawNo={}", withdraw.getId(), withdraw.getWithdrawNo());
                UnifiedTransferService.TransferContext ctx = unifiedTransferService.buildContextForMerchantWithdraw(withdraw.getId());
                unifiedTransferService.executeTransfer(ctx);
            } catch (Exception e) {
                log.error("重试商户提现失败(统一链路): id={}", withdraw.getId(), e);
            }
        }
    }

    @Override
    public BigDecimal calculateFee(BigDecimal amount, Integer withdrawType) {
        WithdrawTypeEnum type = WithdrawTypeEnum.getByCode(withdrawType);
        if (type == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的提现类型");
        }
        return amount.multiply(type.getFeeRate()).divide(HUNDRED, 2, BigDecimal.ROUND_HALF_UP);
    }

    private MerchantWithdrawVO convertToVO(MerchantWithdraw withdraw) {
        MerchantWithdrawVO vo = BeanUtil.copyProperties(withdraw, MerchantWithdrawVO.class);
        MerchantWithdrawStatusEnum statusEnum = MerchantWithdrawStatusEnum.getByCode(withdraw.getWithdrawStatus());
        vo.setWithdrawStatusDesc(statusEnum != null ? statusEnum.getDesc() : "");
        WithdrawTypeEnum typeEnum = WithdrawTypeEnum.getByCode(withdraw.getWithdrawType());
        vo.setWithdrawTypeDesc(typeEnum != null ? typeEnum.getDesc() : "");
        return vo;
    }

    private MerchantInfo getMerchantInfo(String merchantNo) {
        try {
            LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MerchantInfo::getMerchantNo, merchantNo);
            return merchantInfoService.getOne(wrapper);
        } catch (Exception e) {
            log.warn("获取商户信息失败: merchantNo={}", merchantNo, e);
            return null;
        }
    }
}
