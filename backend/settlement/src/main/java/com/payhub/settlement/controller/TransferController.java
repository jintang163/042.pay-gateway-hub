package com.payhub.settlement.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.channel.transfer.TransferResult;
import com.payhub.common.context.CurrentUserContext;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.Result;
import com.payhub.common.result.ResultCode;
import com.payhub.settlement.dto.TransferQueryResponse;
import com.payhub.settlement.entity.AgentWithdraw;
import com.payhub.settlement.entity.PaySplitDetail;
import com.payhub.settlement.entity.SettlementRecord;
import com.payhub.settlement.enums.AgentWithdrawStatusEnum;
import com.payhub.settlement.mapper.AgentWithdrawMapper;
import com.payhub.settlement.mapper.PaySplitDetailMapper;
import com.payhub.settlement.mapper.SettlementRecordMapper;
import com.payhub.settlement.service.SplitTransferService;
import com.payhub.settlement.service.UnifiedTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/transfer")
public class TransferController {

    @Autowired
    private UnifiedTransferService unifiedTransferService;

    @Autowired
    private SplitTransferService splitTransferService;

    @Autowired
    private PaySplitDetailMapper paySplitDetailMapper;

    @Autowired
    private AgentWithdrawMapper agentWithdrawMapper;

    @Autowired
    private SettlementRecordMapper settlementRecordMapper;

    @PostMapping("/query")
    public Result<TransferQueryResponse> queryByTransferNo(
            @RequestParam String transferNo,
            @RequestParam(required = false) String channelTransferNo,
            @RequestParam(required = false) String channel) {
        if (StrUtil.isBlank(transferNo)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "transferNo 不能为空");
        }

        PaySplitDetail detail = findSplitDetailByTransferNo(transferNo);
        if (detail != null) {
            String ch = StrUtil.isNotBlank(channel) ? channel : detail.getTransferChannel();
            String ctNo = StrUtil.isNotBlank(channelTransferNo) ? channelTransferNo : detail.getChannelTransferNo();
            TransferResult r = unifiedTransferService.queryTransferStatus(transferNo, ctNo, ch);
            if (TransferResult.STATUS_PROCESSING.equals(r.getStatus())
                    || TransferResult.STATUS_SUCCESS.equals(r.getStatus())
                    || TransferResult.STATUS_FAIL.equals(r.getStatus())) {
                syncSplitDetailResult(detail.getId(), r);
            }
            TransferQueryResponse resp = convertSplitDetail(detail);
            applyChannelResult(resp, r);
            return Result.success(resp);
        }

        AgentWithdraw withdraw = findWithdrawByTransferNo(transferNo);
        if (withdraw != null) {
            String ch = StrUtil.isNotBlank(channel) ? channel : withdraw.getTransferChannel();
            String ctNo = StrUtil.isNotBlank(channelTransferNo) ? channelTransferNo : withdraw.getChannelTransferNo();
            TransferResult r = unifiedTransferService.queryTransferStatus(transferNo, ctNo, ch);
            syncWithdrawResult(withdraw.getId(), r);
            TransferQueryResponse resp = convertWithdraw(withdraw);
            applyChannelResult(resp, r);
            return Result.success(resp);
        }

        SettlementRecord settlement = findSettlementByTransferNo(transferNo);
        if (settlement != null) {
            String ch = StrUtil.isNotBlank(channel) ? channel : "UNION_PAY";
            String ctNo = channelTransferNo;
            TransferResult r = unifiedTransferService.queryTransferStatus(transferNo, ctNo, ch);
            syncSettlementResult(settlement.getId(), r);
            TransferQueryResponse resp = convertSettlement(settlement);
            applyChannelResult(resp, r);
            return Result.success(resp);
        }

        TransferResult r = unifiedTransferService.queryTransferStatus(transferNo, channelTransferNo, channel);
        TransferQueryResponse resp = new TransferQueryResponse();
        resp.setTransferNo(transferNo);
        resp.setChannelTransferNo(channelTransferNo);
        resp.setChannel(channel);
        applyChannelResult(resp, r);
        return Result.success(resp);
    }

    @PostMapping("/retry")
    public Result<Map<String, Object>> retryTransfer(
            @RequestParam(required = false) Long splitDetailId,
            @RequestParam(required = false) Long withdrawId,
            @RequestParam(required = false) Long settlementId) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        Map<String, Object> result = new HashMap<>();

        if (splitDetailId != null) {
            PaySplitDetail detail = paySplitDetailMapper.selectById(splitDetailId);
            if (detail == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "分账明细不存在");
            }
            if (!detail.getMerchantNo().equals(merchantNo)) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权操作该记录");
            }
            boolean success = splitTransferService.retryTransfer(splitDetailId);
            result.put("sourceType", "SPLIT_DETAIL");
            result.put("sourceId", splitDetailId);
            result.put("success", success);
            return Result.success(result);
        }

        if (withdrawId != null) {
            AgentWithdraw w = agentWithdrawMapper.selectById(withdrawId);
            if (w == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "提现申请不存在");
            }
            if (!w.getMerchantNo().equals(merchantNo)) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权操作该记录");
            }
            UnifiedTransferService.TransferContext ctx = unifiedTransferService.buildContextForAgentWithdraw(withdrawId);
            TransferResult r = unifiedTransferService.executeTransfer(ctx);
            result.put("sourceType", "AGENT_WITHDRAW");
            result.put("sourceId", withdrawId);
            result.put("success", r.isSuccess());
            return Result.success(result);
        }

        if (settlementId != null) {
            SettlementRecord s = settlementRecordMapper.selectById(settlementId);
            if (s == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "结算记录不存在");
            }
            if (!s.getMerchantNo().equals(merchantNo)) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权操作该记录");
            }
            UnifiedTransferService.TransferContext ctx = unifiedTransferService.buildContextForSettlementRecord(settlementId);
            TransferResult r = unifiedTransferService.executeTransfer(ctx);
            result.put("sourceType", "SETTLEMENT");
            result.put("sourceId", settlementId);
            result.put("success", r.isSuccess());
            return Result.success(result);
        }

        throw new BusinessException(ResultCode.PARAM_ERROR, "必须指定一个重试目标");
    }

    @GetMapping("/pending")
    public Result<IPage<PaySplitDetail>> pendingSplitTransfers(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Integer transferStatus) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        Map<String, Object> params = new HashMap<>();
        params.put("merchantNo", merchantNo);
        params.put("orderNo", orderNo);
        params.put("transferStatus", transferStatus);
        return Result.success(splitTransferService.listPendingTransfers(current, size, params));
    }

    @PostMapping("/execute-split")
    public Result<Map<String, Object>> executeSplitById(@RequestParam Long splitDetailId) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        PaySplitDetail detail = paySplitDetailMapper.selectById(splitDetailId);
        if (detail == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分账明细不存在");
        }
        if (!detail.getMerchantNo().equals(merchantNo)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED, "无权操作该记录");
        }
        boolean success = splitTransferService.retryTransfer(splitDetailId);
        Map<String, Object> result = new HashMap<>();
        result.put("splitDetailId", splitDetailId);
        result.put("success", success);
        return Result.success(result);
    }

    private PaySplitDetail findSplitDetailByTransferNo(String transferNo) {
        LambdaQueryWrapper<PaySplitDetail> w = new LambdaQueryWrapper<>();
        w.eq(PaySplitDetail::getTransferNo, transferNo);
        return paySplitDetailMapper.selectOne(w);
    }

    private AgentWithdraw findWithdrawByTransferNo(String transferNo) {
        LambdaQueryWrapper<AgentWithdraw> w = new LambdaQueryWrapper<>();
        w.eq(AgentWithdraw::getTransferNo, transferNo);
        return agentWithdrawMapper.selectOne(w);
    }

    private SettlementRecord findSettlementByTransferNo(String transferNo) {
        LambdaQueryWrapper<SettlementRecord> w = new LambdaQueryWrapper<>();
        w.eq(SettlementRecord::getSplitTransferNo, transferNo);
        return settlementRecordMapper.selectOne(w);
    }

    private void syncSplitDetailResult(Long id, TransferResult r) {
        PaySplitDetail detail = paySplitDetailMapper.selectById(id);
        if (detail == null) return;
        if (r == null) return;
        if (TransferResult.STATUS_SUCCESS.equals(r.getStatus())) {
            detail.setTransferStatus(2);
            detail.setTransferTime(r.getCompleteTime());
            detail.setChannelTransferNo(r.getChannelTransferNo());
            detail.setTransferFailReason(null);
            detail.setStatus(1);
            detail.setSettleTime(r.getCompleteTime());
        } else if (TransferResult.STATUS_FAIL.equals(r.getStatus())) {
            detail.setTransferStatus(3);
            detail.setChannelTransferNo(r.getChannelTransferNo());
            detail.setTransferFailReason(r.getFailReason());
        } else if (TransferResult.STATUS_PROCESSING.equals(r.getStatus())) {
            detail.setTransferStatus(1);
            detail.setChannelTransferNo(r.getChannelTransferNo());
        }
        paySplitDetailMapper.updateById(detail);
    }

    private void syncWithdrawResult(Long id, TransferResult r) {
        AgentWithdraw w = agentWithdrawMapper.selectById(id);
        if (w == null || r == null) return;
        if (TransferResult.STATUS_SUCCESS.equals(r.getStatus())) {
            w.setWithdrawStatus(AgentWithdrawStatusEnum.SUCCESS.getCode());
            w.setTransferTime(r.getCompleteTime());
            w.setChannelTransferNo(r.getChannelTransferNo());
            w.setTransferFailReason(null);
        } else if (TransferResult.STATUS_FAIL.equals(r.getStatus())) {
            w.setWithdrawStatus(AgentWithdrawStatusEnum.FAILED.getCode());
            w.setChannelTransferNo(r.getChannelTransferNo());
            w.setTransferFailReason(r.getFailReason());
        } else if (TransferResult.STATUS_PROCESSING.equals(r.getStatus())) {
            w.setWithdrawStatus(AgentWithdrawStatusEnum.TRANSFERRING.getCode());
            w.setChannelTransferNo(r.getChannelTransferNo());
        }
        agentWithdrawMapper.updateById(w);
    }

    private void syncSettlementResult(Long id, TransferResult r) {
        SettlementRecord s = settlementRecordMapper.selectById(id);
        if (s == null || r == null) return;
        if (TransferResult.STATUS_SUCCESS.equals(r.getStatus())) {
            s.setSettleStatus(2);
            s.setSettleTime(r.getCompleteTime());
            s.setFailReason(null);
        } else if (TransferResult.STATUS_FAIL.equals(r.getStatus())) {
            s.setSettleStatus(3);
            s.setFailReason(r.getFailReason());
        } else if (TransferResult.STATUS_PROCESSING.equals(r.getStatus())) {
            s.setSettleStatus(1);
        }
        settlementRecordMapper.updateById(s);
    }

    private TransferQueryResponse convertSplitDetail(PaySplitDetail d) {
        TransferQueryResponse r = new TransferQueryResponse();
        r.setTransferNo(d.getTransferNo());
        r.setChannelTransferNo(d.getChannelTransferNo());
        r.setChannel(d.getTransferChannel());
        r.setReceiverAccount(d.getReceiverAccount());
        r.setReceiverName(d.getReceiverName());
        r.setRetryCount(d.getTransferRetryCount());
        r.setSourceType("SPLIT_DETAIL");
        r.setSourceNo(d.getSplitDetailNo());
        r.setTransferTime(d.getTransferTime());
        r.setFailReason(d.getTransferFailReason());
        if (d.getSplitAmount() != null) {
            r.setAmountFen(d.getSplitAmount());
            try {
                r.setAmountYuan(d.getSplitAmount().divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP));
            } catch (Exception ignored) {
            }
        }
        Integer st = d.getTransferStatus();
        if (st == null) {
            r.setStatus("PENDING");
            r.setStatusDesc("待处理");
        } else {
            switch (st) {
                case 0:
                    r.setStatus("PENDING");
                    r.setStatusDesc("待处理");
                    break;
                case 1:
                    r.setStatus("PROCESSING");
                    r.setStatusDesc("处理中");
                    break;
                case 2:
                    r.setStatus("SUCCESS");
                    r.setStatusDesc("成功");
                    break;
                case 3:
                    r.setStatus("FAIL");
                    r.setStatusDesc("失败");
                    break;
                default:
                    r.setStatus("UNKNOWN");
                    r.setStatusDesc("未知");
            }
        }
        return r;
    }

    private TransferQueryResponse convertWithdraw(AgentWithdraw w) {
        TransferQueryResponse r = new TransferQueryResponse();
        r.setTransferNo(w.getTransferNo());
        r.setChannelTransferNo(w.getChannelTransferNo());
        r.setChannel(w.getTransferChannel());
        r.setReceiverAccount(w.getBankAccount());
        r.setReceiverName(w.getAccountName());
        r.setRetryCount(w.getTransferRetryCount());
        r.setSourceType("AGENT_WITHDRAW");
        r.setSourceNo(w.getWithdrawNo());
        r.setTransferTime(w.getTransferTime());
        r.setFailReason(w.getTransferFailReason());
        r.setFeeAmount(w.getFeeAmount());
        if (w.getActualAmount() != null) {
            r.setAmountYuan(w.getActualAmount());
            r.setAmountFen(w.getActualAmount().multiply(new BigDecimal("100")).setScale(0, BigDecimal.ROUND_HALF_UP));
        }
        AgentWithdrawStatusEnum statusEnum = AgentWithdrawStatusEnum.getByCode(w.getWithdrawStatus());
        if (statusEnum != null) {
            r.setStatus(statusEnum.name());
            r.setStatusDesc(statusEnum.getDesc());
        }
        return r;
    }

    private TransferQueryResponse convertSettlement(SettlementRecord s) {
        TransferQueryResponse r = new TransferQueryResponse();
        r.setTransferNo(s.getSplitTransferNo());
        r.setChannel(s.getPayChannel());
        r.setReceiverAccount(s.getBankAccount());
        r.setReceiverName(s.getAccountName());
        r.setRetryCount(s.getRetryCount());
        r.setSourceType("SETTLEMENT");
        r.setSourceNo(s.getSettlementNo());
        r.setTransferTime(s.getSettleTime());
        r.setFailReason(s.getFailReason());
        if (s.getActualSettleAmount() != null) {
            r.setAmountYuan(s.getActualSettleAmount());
            r.setAmountFen(s.getActualSettleAmount().multiply(new BigDecimal("100")).setScale(0, BigDecimal.ROUND_HALF_UP));
        }
        Integer st = s.getSettleStatus();
        if (st == null) {
            r.setStatus("PENDING");
            r.setStatusDesc("待处理");
        } else {
            switch (st) {
                case 0:
                    r.setStatus("PENDING");
                    r.setStatusDesc("待结算");
                    break;
                case 1:
                    r.setStatus("PROCESSING");
                    r.setStatusDesc("结算中");
                    break;
                case 2:
                    r.setStatus("SUCCESS");
                    r.setStatusDesc("已结算");
                    break;
                case 3:
                    r.setStatus("FAIL");
                    r.setStatusDesc("结算失败");
                    break;
                default:
                    r.setStatus("UNKNOWN");
                    r.setStatusDesc("未知");
            }
        }
        return r;
    }

    private void applyChannelResult(TransferQueryResponse resp, TransferResult r) {
        if (r == null) return;
        if (resp.getChannelTransferNo() == null) {
            resp.setChannelTransferNo(r.getChannelTransferNo());
        }
        if (r.isSuccess()) {
            resp.setStatus("SUCCESS");
            resp.setStatusDesc("成功");
        } else if (TransferResult.STATUS_PROCESSING.equals(r.getStatus())) {
            resp.setStatus("PROCESSING");
            resp.setStatusDesc("处理中");
        } else if (TransferResult.STATUS_FAIL.equals(r.getStatus())) {
            resp.setStatus("FAIL");
            resp.setStatusDesc("失败");
        }
        resp.setFailCode(r.getFailCode());
        if (StrUtil.isNotBlank(r.getFailReason()) && StrUtil.isBlank(resp.getFailReason())) {
            resp.setFailReason(r.getFailReason());
        }
        if (r.getFeeAmount() != null) {
            resp.setFeeAmount(r.getFeeAmount());
        }
        if (r.getCompleteTime() != null) {
            resp.setTransferTime(r.getCompleteTime());
        }
    }
}
