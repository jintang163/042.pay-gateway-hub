package com.payhub.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.settlement.dto.AgentWithdrawApplyRequest;
import com.payhub.settlement.dto.AgentWithdrawAuditRequest;
import com.payhub.settlement.dto.AgentWithdrawVO;
import com.payhub.settlement.entity.AgentWithdraw;
import com.payhub.settlement.enums.AgentWithdrawStatusEnum;
import com.payhub.settlement.mapper.AgentWithdrawMapper;
import com.payhub.settlement.service.AgentProfitService;
import com.payhub.settlement.service.AgentWithdrawService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AgentWithdrawServiceImpl extends ServiceImpl<AgentWithdrawMapper, AgentWithdraw> implements AgentWithdrawService {

    @Autowired
    private AgentProfitService agentProfitService;

    private static final BigDecimal MIN_WITHDRAW_AMOUNT = new BigDecimal("10");

    private static final BigDecimal WITHDRAW_FEE_RATE = new BigDecimal("0");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyWithdraw(AgentWithdrawApplyRequest request) {
        BigDecimal availableBalance = getAvailableBalance(request.getMerchantNo());
        if (request.getWithdrawAmount().compareTo(MIN_WITHDRAW_AMOUNT) < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "提现金额不能小于最低提现金额");
        }
        if (request.getWithdrawAmount().compareTo(availableBalance) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "可提现余额不足");
        }
        BigDecimal feeAmount = request.getWithdrawAmount().multiply(WITHDRAW_FEE_RATE).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal actualAmount = request.getWithdrawAmount().subtract(feeAmount);
        AgentWithdraw withdraw = new AgentWithdraw();
        withdraw.setWithdrawNo(OrderNoGenerator.generateWithPrefix("AW"));
        withdraw.setMerchantNo(request.getMerchantNo());
        withdraw.setWithdrawAmount(request.getWithdrawAmount());
        withdraw.setActualAmount(actualAmount);
        withdraw.setFeeAmount(feeAmount);
        withdraw.setWithdrawStatus(AgentWithdrawStatusEnum.PENDING.getCode());
        withdraw.setBankName(request.getBankName());
        withdraw.setBankAccount(request.getBankAccount());
        withdraw.setAccountName(request.getAccountName());
        withdraw.setRemark(request.getRemark());
        this.save(withdraw);
        log.info("佣金提现申请提交成功: withdrawNo={}, merchantNo={}, amount={}", withdraw.getWithdrawNo(), request.getMerchantNo(), request.getWithdrawAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditWithdraw(AgentWithdrawAuditRequest request) {
        AgentWithdraw withdraw = this.getById(request.getId());
        if (withdraw == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "提现申请不存在");
        }
        if (!AgentWithdrawStatusEnum.PENDING.getCode().equals(withdraw.getWithdrawStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前状态不允许审核");
        }
        if (request.getAuditStatus() == 1) {
            withdraw.setWithdrawStatus(AgentWithdrawStatusEnum.APPROVED.getCode());
        } else {
            withdraw.setWithdrawStatus(AgentWithdrawStatusEnum.REJECTED.getCode());
        }
        withdraw.setAuditUser(request.getAuditUser());
        withdraw.setAuditRemark(request.getAuditRemark());
        withdraw.setAuditTime(LocalDateTime.now());
        this.updateById(withdraw);
        log.info("提现审核完成: id={}, status={}", request.getId(), request.getAuditStatus());
    }

    @Override
    public AgentWithdrawVO getWithdrawById(Long id) {
        AgentWithdraw withdraw = this.getById(id);
        if (withdraw == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "提现申请不存在");
        }
        return convertToVO(withdraw);
    }

    @Override
    public AgentWithdrawVO getWithdrawByWithdrawNo(String withdrawNo) {
        AgentWithdraw withdraw = baseMapper.selectByWithdrawNo(withdrawNo);
        return withdraw != null ? convertToVO(withdraw) : null;
    }

    @Override
    public IPage<AgentWithdrawVO> listPage(Long current, Long size, Map<String, Object> params) {
        Page<AgentWithdraw> page = new Page<>(current, size);
        IPage<AgentWithdraw> withdrawPage = baseMapper.selectPageList(page, params);
        return withdrawPage.convert(this::convertToVO);
    }

    @Override
    public List<AgentWithdrawVO> listByMerchantNo(String merchantNo) {
        List<AgentWithdraw> withdraws = baseMapper.selectByMerchantNo(merchantNo);
        return withdraws.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public BigDecimal getTotalWithdraw(String merchantNo, Integer withdrawStatus) {
        BigDecimal total = baseMapper.selectTotalWithdrawByMerchant(merchantNo, withdrawStatus);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getAvailableBalance(String merchantNo) {
        BigDecimal totalProfit = agentProfitService.getTotalProfit(merchantNo, null);
        BigDecimal totalWithdraw = getTotalWithdraw(merchantNo, null);
        BigDecimal pendingWithdraw = getTotalWithdraw(merchantNo, AgentWithdrawStatusEnum.PENDING.getCode());
        BigDecimal approvedWithdraw = getTotalWithdraw(merchantNo, AgentWithdrawStatusEnum.APPROVED.getCode());
        BigDecimal successWithdraw = getTotalWithdraw(merchantNo, AgentWithdrawStatusEnum.SUCCESS.getCode());
        BigDecimal transferringWithdraw = getTotalWithdraw(merchantNo, AgentWithdrawStatusEnum.TRANSFERRING.getCode());
        BigDecimal usedWithdraw = pendingWithdraw.add(approvedWithdraw).add(successWithdraw).add(transferringWithdraw);
        return totalProfit.subtract(usedWithdraw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeTransfer(Long id) {
        AgentWithdraw withdraw = this.getById(id);
        if (withdraw == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "提现申请不存在");
        }
        if (!AgentWithdrawStatusEnum.APPROVED.getCode().equals(withdraw.getWithdrawStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前状态不允许转账");
        }
        withdraw.setWithdrawStatus(AgentWithdrawStatusEnum.TRANSFERRING.getCode());
        this.updateById(withdraw);
        withdraw.setWithdrawStatus(AgentWithdrawStatusEnum.SUCCESS.getCode());
        withdraw.setTransferNo(OrderNoGenerator.generateWithPrefix("TFR"));
        withdraw.setTransferTime(LocalDateTime.now());
        this.updateById(withdraw);
        log.info("提现转账执行完成: id={}, withdrawNo={}", id, withdraw.getWithdrawNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryFailedWithdraw() {
        LambdaQueryWrapper<AgentWithdraw> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentWithdraw::getWithdrawStatus, AgentWithdrawStatusEnum.FAILED.getCode());
        List<AgentWithdraw> failedList = this.list(wrapper);
        for (AgentWithdraw withdraw : failedList) {
            try {
                withdraw.setTransferRetryCount(withdraw.getTransferRetryCount() != null ? withdraw.getTransferRetryCount() + 1 : 1);
                withdraw.setWithdrawStatus(AgentWithdrawStatusEnum.TRANSFERRING.getCode());
                this.updateById(withdraw);
                withdraw.setWithdrawStatus(AgentWithdrawStatusEnum.SUCCESS.getCode());
                withdraw.setTransferTime(LocalDateTime.now());
                this.updateById(withdraw);
                log.info("提现重试成功: id={}", withdraw.getId());
            } catch (Exception e) {
                log.error("提现重试失败: id={}", withdraw.getId(), e);
            }
        }
    }

    private AgentWithdrawVO convertToVO(AgentWithdraw withdraw) {
        AgentWithdrawVO vo = BeanUtil.copyProperties(withdraw, AgentWithdrawVO.class);
        AgentWithdrawStatusEnum statusEnum = AgentWithdrawStatusEnum.getByCode(withdraw.getWithdrawStatus());
        vo.setWithdrawStatusDesc(statusEnum != null ? statusEnum.getDesc() : "");
        return vo;
    }
}
