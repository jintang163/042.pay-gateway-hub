package com.payhub.settlement.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payhub.channel.transfer.TransferRequest;
import com.payhub.channel.transfer.TransferResult;
import com.payhub.channel.transfer.TransferService;
import com.payhub.channel.transfer.TransferServiceFactory;
import com.payhub.common.context.SandboxContext;
import com.payhub.common.enums.SandboxSceneEnum;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.merchant.entity.MerchantInfo;
import com.payhub.merchant.service.MerchantInfoService;
import com.payhub.settlement.entity.AgentWithdraw;
import com.payhub.settlement.entity.MerchantWithdraw;
import com.payhub.settlement.entity.PaySplitDetail;
import com.payhub.settlement.entity.SettlementRecord;
import com.payhub.settlement.entity.SplitReceiver;
import com.payhub.settlement.enums.AgentWithdrawStatusEnum;
import com.payhub.settlement.enums.MerchantWithdrawStatusEnum;
import com.payhub.settlement.enums.SplitReceiverVerifyStatusEnum;
import com.payhub.settlement.mapper.AgentWithdrawMapper;
import com.payhub.settlement.mapper.MerchantWithdrawMapper;
import com.payhub.settlement.mapper.PaySplitDetailMapper;
import com.payhub.settlement.mapper.SettlementRecordMapper;
import com.payhub.settlement.service.SplitReceiverService;
import com.payhub.settlement.service.UnifiedTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
public class UnifiedTransferServiceImpl implements UnifiedTransferService {

    public static final String SOURCE_SPLIT_DETAIL = "SPLIT_DETAIL";
    public static final String SOURCE_AGENT_WITHDRAW = "AGENT_WITHDRAW";
    public static final String SOURCE_SETTLEMENT = "SETTLEMENT";
    public static final String SOURCE_MERCHANT_WITHDRAW = "MERCHANT_WITHDRAW";

    public static final int TRANSFER_STATUS_PENDING = 0;
    public static final int TRANSFER_STATUS_PROCESSING = 1;
    public static final int TRANSFER_STATUS_SUCCESS = 2;
    public static final int TRANSFER_STATUS_FAIL = 3;

    @Value("${payhub.split.transfer.default-channel:UNION_PAY}")
    private String defaultChannel;

    @Value("${payhub.split.transfer.idcard-verify-required:true}")
    private boolean idCardVerifyRequired;

    @Autowired(required = false)
    private TransferServiceFactory transferServiceFactory;

    @Autowired
    private PaySplitDetailMapper paySplitDetailMapper;

    @Autowired
    private AgentWithdrawMapper agentWithdrawMapper;

    @Autowired
    private SettlementRecordMapper settlementRecordMapper;

    @Autowired
    private MerchantWithdrawMapper merchantWithdrawMapper;

    @Autowired
    private SplitReceiverService splitReceiverService;

    @Autowired(required = false)
    private MerchantInfoService merchantInfoService;

    @Override
    public TransferContext buildContextForSplitDetail(Long splitDetailId) {
        PaySplitDetail detail = paySplitDetailMapper.selectById(splitDetailId);
        if (detail == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分账明细不存在");
        }

        TransferContext ctx = new TransferContext();
        ctx.setSourceId(detail.getId());
        ctx.setSourceType(SOURCE_SPLIT_DETAIL);
        ctx.setSourceNo(detail.getSplitDetailNo());
        ctx.setTransferNo(StrUtil.isNotBlank(detail.getTransferNo())
                ? detail.getTransferNo() : OrderNoGenerator.generateWithPrefix("TF"));
        ctx.setMerchantNo(detail.getMerchantNo());
        ctx.setRemark(StrUtil.isNotBlank(detail.getRemark()) ? detail.getRemark() : "分账代付");

        String channel = StrUtil.isNotBlank(detail.getTransferChannel())
                ? detail.getTransferChannel() : resolveChannelByReceiver(detail.getReceiverAccount());
        ctx.setChannel(channel);

        fillReceiverBySplitReceiver(ctx, detail.getReceiverAccount(), detail.getMerchantNo());

        if (ctx.getAmountFen() == null && detail.getSplitAmount() != null) {
            ctx.setAmountFen(normalizeAmountToFen(detail.getSplitAmount(), "SPLIT_AMOUNT"));
        }

        if (ctx.getReceiverName() == null) {
            ctx.setReceiverName(detail.getReceiverName());
        }
        if (ctx.getReceiverAccount() == null) {
            ctx.setReceiverAccount(detail.getReceiverAccount());
        }

        return ctx;
    }

    @Override
    public TransferContext buildContextForAgentWithdraw(Long withdrawId) {
        AgentWithdraw withdraw = agentWithdrawMapper.selectById(withdrawId);
        if (withdraw == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "提现申请不存在");
        }

        TransferContext ctx = new TransferContext();
        ctx.setSourceId(withdraw.getId());
        ctx.setSourceType(SOURCE_AGENT_WITHDRAW);
        ctx.setSourceNo(withdraw.getWithdrawNo());
        ctx.setTransferNo(StrUtil.isNotBlank(withdraw.getTransferNo())
                ? withdraw.getTransferNo() : OrderNoGenerator.generateWithPrefix("TF"));
        ctx.setMerchantNo(withdraw.getMerchantNo());
        ctx.setChannel(StrUtil.isNotBlank(withdraw.getTransferChannel())
                ? withdraw.getTransferChannel() : resolveChannelByReceiver(withdraw.getBankAccount()));
        ctx.setReceiverAccount(withdraw.getBankAccount());
        ctx.setReceiverName(withdraw.getAccountName());
        ctx.setBankName(withdraw.getBankName());
        ctx.setAmountFen(normalizeAmountToFen(withdraw.getActualAmount(), "WITHDRAW_AMOUNT"));
        ctx.setRemark(StrUtil.isNotBlank(withdraw.getRemark()) ? withdraw.getRemark() : "佣金提现");
        ctx.setSkipIdCardVerify(true);
        return ctx;
    }

    @Override
    public TransferContext buildContextForSettlementRecord(Long settlementId) {
        SettlementRecord record = settlementRecordMapper.selectById(settlementId);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "结算记录不存在");
        }

        TransferContext ctx = new TransferContext();
        ctx.setSourceId(record.getId());
        ctx.setSourceType(SOURCE_SETTLEMENT);
        ctx.setSourceNo(record.getSettlementNo());
        ctx.setTransferNo(StrUtil.isNotBlank(record.getSplitTransferNo())
                ? record.getSplitTransferNo() : OrderNoGenerator.generateWithPrefix("TF"));
        ctx.setMerchantNo(record.getMerchantNo());
        ctx.setChannel(resolveChannelByReceiver(record.getBankAccount()));
        ctx.setReceiverAccount(record.getBankAccount());
        ctx.setReceiverName(record.getAccountName());
        ctx.setBankName(record.getBankName());
        ctx.setAmountFen(normalizeAmountToFen(record.getActualSettleAmount(), "SETTLE_AMOUNT"));
        ctx.setRemark("商户结算-" + record.getSettleDate());
        ctx.setSkipIdCardVerify(true);

        if (StrUtil.isBlank(ctx.getReceiverAccount()) || StrUtil.isBlank(ctx.getReceiverName())) {
            MerchantInfo merchant = getMerchantInfo(record.getMerchantNo());
            if (merchant != null) {
                if (StrUtil.isBlank(ctx.getReceiverAccount())) {
                    ctx.setReceiverAccount(merchant.getSettlementBankAccount());
                }
                if (StrUtil.isBlank(ctx.getReceiverName())) {
                    ctx.setReceiverName(merchant.getSettlementAccountName());
                }
                if (StrUtil.isBlank(ctx.getBankName())) {
                    ctx.setBankName(merchant.getSettlementBankName());
                }
            }
        }
        return ctx;
    }

    @Override
    public TransferContext buildContextForMerchantWithdraw(Long withdrawId) {
        MerchantWithdraw withdraw = merchantWithdrawMapper.selectById(withdrawId);
        if (withdraw == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "提现申请不存在");
        }

        TransferContext ctx = new TransferContext();
        ctx.setSourceId(withdraw.getId());
        ctx.setSourceType(SOURCE_MERCHANT_WITHDRAW);
        ctx.setSourceNo(withdraw.getWithdrawNo());
        ctx.setTransferNo(StrUtil.isNotBlank(withdraw.getTransferNo())
                ? withdraw.getTransferNo() : OrderNoGenerator.generateWithPrefix("TF"));
        ctx.setMerchantNo(withdraw.getMerchantNo());
        ctx.setChannel(StrUtil.isNotBlank(withdraw.getTransferChannel())
                ? withdraw.getTransferChannel() : resolveChannelByReceiver(withdraw.getBankAccount()));
        ctx.setReceiverAccount(withdraw.getBankAccount());
        ctx.setReceiverName(withdraw.getAccountName());
        ctx.setBankName(withdraw.getBankName());
        ctx.setAmountFen(normalizeAmountToFen(withdraw.getActualAmount(), "WITHDRAW_AMOUNT"));
        ctx.setRemark(StrUtil.isNotBlank(withdraw.getRemark()) ? withdraw.getRemark() : "商户提现");
        ctx.setSkipIdCardVerify(true);
        return ctx;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransferResult executeTransfer(TransferContext ctx) {
        if (ctx == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "代付上下文不能为空");
        }
        if (ctx.getAmountFen() == null || ctx.getAmountFen().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "代付金额必须大于0");
        }
        if (StrUtil.isBlank(ctx.getReceiverAccount())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "收款账户不能为空");
        }
        if (StrUtil.isBlank(ctx.getChannel())) {
            ctx.setChannel(defaultChannel);
        }

        if (Boolean.TRUE.equals(idCardVerifyRequired) && !Boolean.TRUE.equals(ctx.getSkipIdCardVerify())) {
            enforceIdCardVerification(ctx);
        }

        markSourceProcessing(ctx);

        TransferRequest request = new TransferRequest();
        request.setTransferNo(ctx.getTransferNo());
        request.setChannel(ctx.getChannel());
        request.setReceiverAccount(ctx.getReceiverAccount());
        request.setReceiverName(ctx.getReceiverName());
        request.setAmount(ctx.getAmountFen());
        request.setBankName(ctx.getBankName());
        request.setBankCode(ctx.getBankCode());
        request.setBankBranchName(ctx.getBankBranchName());
        request.setReceiverType(ctx.getReceiverType());
        request.setIdCardNo(ctx.getIdCardNo());
        request.setIdCardName(ctx.getIdCardName());
        request.setBankPhone(ctx.getBankPhone());
        request.setMerchantNo(ctx.getMerchantNo());
        request.setSourceType(ctx.getSourceType());
        request.setSourceNo(ctx.getSourceNo());
        request.setRemark(ctx.getRemark());

        TransferResult result = callChannelTransfer(ctx.getChannel(), request);
        applyResultToSource(ctx, result);

        log.info("统一代付执行完成: sourceType={}, sourceId={}, transferNo={}, channel={}, success={}, status={}",
                ctx.getSourceType(), ctx.getSourceId(), ctx.getTransferNo(), ctx.getChannel(),
                result.isSuccess(), result.getStatus());
        return result;
    }

    @Override
    public TransferResult queryTransferStatus(String transferNo, String channelTransferNo, String channel) {
        if (StrUtil.isBlank(channel)) {
            channel = defaultChannel;
        }
        if (transferServiceFactory == null) {
            log.warn("TransferServiceFactory 未注入，使用沙箱查询模拟: transferNo={}", transferNo);
            return TransferResult.success(transferNo, channelTransferNo, LocalDateTime.now());
        }
        return transferServiceFactory.queryTransfer(channel, transferNo, channelTransferNo);
    }

    private void enforceIdCardVerification(TransferContext ctx) {
        if (StrUtil.isBlank(ctx.getMerchantNo()) || StrUtil.isBlank(ctx.getSourceNo())
                && StrUtil.isBlank(ctx.getReceiverAccount())) {
            return;
        }

        String receiverAccount = ctx.getReceiverAccount();
        SplitReceiver receiver = null;
        try {
            receiver = splitReceiverService.checkReceiverVerified(receiverAccount, ctx.getMerchantNo());
        } catch (Exception e) {
            log.warn("未找到接收方银行卡认证记录, receiverAccount={}, merchantNo={}", receiverAccount, ctx.getMerchantNo());
        }

        if (receiver != null) {
            if (receiver.getIdCardVerifyStatus() == null
                    || !SplitReceiverVerifyStatusEnum.VERIFIED.getCode().equals(receiver.getIdCardVerifyStatus())) {
                log.error("接收方未通过身份证核验, receiverNo={}, idCardVerifyStatus={}",
                        receiver.getReceiverNo(), receiver.getIdCardVerifyStatus());
                throw new BusinessException(ResultCode.PARAM_ERROR,
                        "接收方[" + (StrUtil.isNotBlank(receiver.getReceiverName())
                                ? receiver.getReceiverName() : receiverAccount)
                                + "]未完成二代/三代身份证核验，请先完成身份证核验后再操作");
            }
        }
    }

    private void fillReceiverBySplitReceiver(TransferContext ctx, String receiverAccount, String merchantNo) {
        try {
            SplitReceiver receiver = splitReceiverService.checkReceiverVerified(receiverAccount, merchantNo);
            if (receiver != null) {
                ctx.setReceiverAccount(receiver.getBankCardNo());
                ctx.setReceiverName(receiver.getIdCardName());
                ctx.setBankName(receiver.getBankName());
                ctx.setBankBranchName(receiver.getBankBranchName());
                ctx.setReceiverType(receiver.getReceiverType());
                ctx.setIdCardNo(receiver.getIdCardNo());
                ctx.setIdCardName(receiver.getIdCardName());
                ctx.setBankPhone(receiver.getBankPhone());
            }
        } catch (Exception e) {
            log.debug("未通过接收方查询到银行卡信息, receiverAccount={}, merchantNo={}", receiverAccount, merchantNo);
        }
    }

    private String resolveChannelByReceiver(String account) {
        if (StrUtil.isBlank(account)) {
            return defaultChannel;
        }
        if (account.startsWith("62")) {
            return "UNION_PAY";
        }
        if (account.contains("@") || account.matches("^1[3-9]\\d{9}$")) {
            return "ALIPAY";
        }
        return defaultChannel;
    }

    private BigDecimal normalizeAmountToFen(BigDecimal amount, String field) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal amountAbs = amount.abs();
        if (amountAbs.compareTo(new BigDecimal("1000000000")) > 0) {
            log.warn("[金额归一化] {}={} 过大, 按分原样使用", field, amount);
            return amount;
        }
        if (amountAbs.compareTo(new BigDecimal("10")) > 0 && amountAbs.scale() <= 0) {
            log.debug("[金额归一化] {}={} 按分处理 (无小数且数值较大)", field, amount);
            return amount;
        }
        BigDecimal converted = amount.multiply(new BigDecimal("100"))
                .setScale(0, BigDecimal.ROUND_HALF_UP);
        log.info("[金额归一化] {}={} 元 -> {} 分", field, amount, converted);
        return converted;
    }

    private TransferResult callChannelTransfer(String channel, TransferRequest request) {
        if (transferServiceFactory == null) {
            String scene = SandboxContext.getScene();
            if (SandboxSceneEnum.TRANSFER_FAIL.getCode().equalsIgnoreCase(scene)) {
                return TransferResult.fail(request.getTransferNo(), "E001", "沙箱强制代付失败");
            }
            if (SandboxSceneEnum.TRANSFER_EXCEPTION.getCode().equalsIgnoreCase(scene)) {
                throw new BusinessException(ResultCode.SYSTEM_ERROR, "沙箱强制代付异常");
            }
            return TransferResult.success(request.getTransferNo(),
                    "SB" + request.getTransferNo(), LocalDateTime.now());
        }

        TransferService service = transferServiceFactory.getTransferService(channel);
        if (service == null) {
            return TransferResult.fail(request.getTransferNo(), "E002", "不支持的代付通道: " + channel);
        }
        return service.transfer(request);
    }

    private void markSourceProcessing(TransferContext ctx) {
        String transferNo = ctx.getTransferNo();
        String channel = ctx.getChannel();
        switch (ctx.getSourceType()) {
            case SOURCE_SPLIT_DETAIL: {
                PaySplitDetail d = paySplitDetailMapper.selectById(ctx.getSourceId());
                if (d != null) {
                    d.setTransferNo(transferNo);
                    d.setTransferChannel(channel);
                    d.setTransferStatus(TRANSFER_STATUS_PROCESSING);
                    int retry = d.getTransferRetryCount() == null ? 0 : d.getTransferRetryCount();
                    d.setTransferRetryCount(retry + 1);
                    paySplitDetailMapper.updateById(d);
                }
                break;
            }
            case SOURCE_AGENT_WITHDRAW: {
                AgentWithdraw w = agentWithdrawMapper.selectById(ctx.getSourceId());
                if (w != null) {
                    w.setTransferNo(transferNo);
                    w.setTransferChannel(channel);
                    w.setWithdrawStatus(AgentWithdrawStatusEnum.TRANSFERRING.getCode());
                    int retry = w.getTransferRetryCount() == null ? 0 : w.getTransferRetryCount();
                    w.setTransferRetryCount(retry + 1);
                    agentWithdrawMapper.updateById(w);
                }
                break;
            }
            case SOURCE_SETTLEMENT: {
                SettlementRecord s = settlementRecordMapper.selectById(ctx.getSourceId());
                if (s != null) {
                    s.setSplitTransferNo(transferNo);
                    s.setSettleStatus(1);
                    int retry = s.getRetryCount() == null ? 0 : s.getRetryCount();
                    s.setRetryCount(retry + 1);
                    settlementRecordMapper.updateById(s);
                }
                break;
            }
            case SOURCE_MERCHANT_WITHDRAW: {
                MerchantWithdraw w = merchantWithdrawMapper.selectById(ctx.getSourceId());
                if (w != null) {
                    w.setTransferNo(transferNo);
                    w.setTransferChannel(channel);
                    w.setWithdrawStatus(MerchantWithdrawStatusEnum.TRANSFERRING.getCode());
                    int retry = w.getTransferRetryCount() == null ? 0 : w.getTransferRetryCount();
                    w.setTransferRetryCount(retry + 1);
                    merchantWithdrawMapper.updateById(w);
                }
                break;
            }
            default:
                break;
        }
    }

    private void applyResultToSource(TransferContext ctx, TransferResult result) {
        boolean success = result.isSuccess();
        String status = result.getStatus();
        LocalDateTime now = LocalDateTime.now();

        switch (ctx.getSourceType()) {
            case SOURCE_SPLIT_DETAIL: {
                PaySplitDetail d = paySplitDetailMapper.selectById(ctx.getSourceId());
                if (d == null) break;
                d.setChannelTransferNo(result.getChannelTransferNo());
                if (success) {
                    d.setTransferStatus(TRANSFER_STATUS_SUCCESS);
                    d.setTransferTime(result.getCompleteTime() != null ? result.getCompleteTime() : now);
                    d.setTransferFailReason(null);
                    d.setNextTransferRetryTime(null);
                    d.setStatus(1);
                    d.setSettleTime(d.getTransferTime());
                } else if (TransferResult.STATUS_PROCESSING.equals(status)) {
                    d.setTransferStatus(TRANSFER_STATUS_PROCESSING);
                } else {
                    d.setTransferStatus(TRANSFER_STATUS_FAIL);
                    d.setTransferFailReason(result.getFailReason());
                    int retry = d.getTransferRetryCount() == null ? 0 : d.getTransferRetryCount();
                    if (retry < 5) {
                        int minutes = (int) Math.pow(2, Math.min(retry, 5));
                        d.setNextTransferRetryTime(now.plusMinutes(minutes));
                    }
                }
                paySplitDetailMapper.updateById(d);
                break;
            }
            case SOURCE_AGENT_WITHDRAW: {
                AgentWithdraw w = agentWithdrawMapper.selectById(ctx.getSourceId());
                if (w == null) break;
                w.setChannelTransferNo(result.getChannelTransferNo());
                if (success) {
                    w.setWithdrawStatus(AgentWithdrawStatusEnum.SUCCESS.getCode());
                    w.setTransferTime(result.getCompleteTime() != null ? result.getCompleteTime() : now);
                    w.setTransferFailReason(null);
                    w.setNextTransferRetryTime(null);
                } else if (TransferResult.STATUS_PROCESSING.equals(status)) {
                    w.setWithdrawStatus(AgentWithdrawStatusEnum.TRANSFERRING.getCode());
                } else {
                    w.setWithdrawStatus(AgentWithdrawStatusEnum.FAILED.getCode());
                    w.setTransferFailReason(result.getFailReason());
                    int retry = w.getTransferRetryCount() == null ? 0 : w.getTransferRetryCount();
                    if (retry < 5) {
                        int minutes = (int) Math.pow(2, Math.min(retry, 5));
                        w.setNextTransferRetryTime(now.plusMinutes(minutes));
                    }
                }
                agentWithdrawMapper.updateById(w);
                break;
            }
            case SOURCE_SETTLEMENT: {
                SettlementRecord s = settlementRecordMapper.selectById(ctx.getSourceId());
                if (s == null) break;
                s.setSplitDetailId(ctx.getSourceId());
                if (success) {
                    s.setSettleStatus(2);
                    s.setSettleTime(result.getCompleteTime() != null ? result.getCompleteTime() : now);
                    s.setFailReason(null);
                    s.setNextRetryTime(null);
                } else if (TransferResult.STATUS_PROCESSING.equals(status)) {
                    s.setSettleStatus(1);
                } else {
                    s.setSettleStatus(3);
                    s.setFailReason(result.getFailReason());
                    int retry = s.getRetryCount() == null ? 0 : s.getRetryCount();
                    if (retry < 5) {
                        int minutes = (int) Math.pow(2, Math.min(retry, 5));
                        s.setNextRetryTime(now.plusMinutes(minutes));
                    }
                }
                settlementRecordMapper.updateById(s);
                break;
            }
            case SOURCE_MERCHANT_WITHDRAW: {
                MerchantWithdraw w = merchantWithdrawMapper.selectById(ctx.getSourceId());
                if (w == null) break;
                w.setChannelTransferNo(result.getChannelTransferNo());
                if (success) {
                    w.setWithdrawStatus(MerchantWithdrawStatusEnum.SUCCESS.getCode());
                    w.setTransferTime(result.getCompleteTime() != null ? result.getCompleteTime() : now);
                    w.setTransferFailReason(null);
                    w.setNextTransferRetryTime(null);
                } else if (TransferResult.STATUS_PROCESSING.equals(status)) {
                    w.setWithdrawStatus(MerchantWithdrawStatusEnum.TRANSFERRING.getCode());
                } else {
                    w.setWithdrawStatus(MerchantWithdrawStatusEnum.FAILED.getCode());
                    w.setTransferFailReason(result.getFailReason());
                    int retry = w.getTransferRetryCount() == null ? 0 : w.getTransferRetryCount();
                    if (retry < 5) {
                        int minutes = (int) Math.pow(2, Math.min(retry, 5));
                        w.setNextTransferRetryTime(now.plusMinutes(minutes));
                    }
                }
                merchantWithdrawMapper.updateById(w);
                break;
            }
            default:
                break;
        }
    }

    private MerchantInfo getMerchantInfo(String merchantNo) {
        if (merchantInfoService == null || StrUtil.isBlank(merchantNo)) {
            return null;
        }
        try {
            LambdaQueryWrapper<MerchantInfo> w = new LambdaQueryWrapper<>();
            w.eq(MerchantInfo::getMerchantNo, merchantNo);
            return merchantInfoService.getOne(w);
        } catch (Exception e) {
            return null;
        }
    }
}
