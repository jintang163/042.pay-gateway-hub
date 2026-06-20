package com.payhub.channel.transfer;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
public class NetBankTransfer implements TransferService {

    @Override
    public TransferResult transfer(TransferRequest request) {
        log.info("[NET_BANK]开始执行网银代付, transferNo={}, bankName={}, bankCode={}, " +
                        "receiverAccount={}, receiverName={}, receiverType={}, amount={}分",
                request.getTransferNo(), request.getBankName(), request.getBankCode(),
                request.getReceiverAccount(), request.getReceiverName(),
                request.getReceiverType(), request.getAmount());

        String channelTransferNo = "NB" + IdUtil.getSnowflakeNextIdStr();

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        double random = Math.random();
        if (random < 0.12) {
            String failCode = "E001";
            String failReason = "账户信息有误或开户行不支持网银代付";
            log.warn("[NET_BANK]网银代付失败, transferNo={}, failCode={}, reason={}",
                    request.getTransferNo(), failCode, failReason);
            TransferResult result = TransferResult.fail(request.getTransferNo(), failCode, failReason);
            result.setFeeAmount(BigDecimal.ZERO);
            return result;
        } else if (random < 0.16) {
            String failCode = "E002";
            String failReason = "收款账户被冻结或受限";
            log.warn("[NET_BANK]网银代付失败, transferNo={}, failCode={}, reason={}",
                    request.getTransferNo(), failCode, failReason);
            TransferResult result = TransferResult.fail(request.getTransferNo(), failCode, failReason);
            result.setFeeAmount(BigDecimal.ZERO);
            return result;
        } else if (random < 0.22) {
            log.info("[NET_BANK]网银代付处理中, transferNo={}, channelTransferNo={}",
                    request.getTransferNo(), channelTransferNo);
            return TransferResult.processing(request.getTransferNo(), channelTransferNo);
        } else {
            log.info("[NET_BANK]网银代付成功, transferNo={}, channelTransferNo={}, amount={}分",
                    request.getTransferNo(), channelTransferNo, request.getAmount());
            TransferResult result = TransferResult.success(request.getTransferNo(), channelTransferNo, LocalDateTime.now());
            result.setFeeAmount(calcFee(request.getAmount()));
            return result;
        }
    }

    @Override
    public String getChannelCode() {
        return "NET_BANK";
    }

    private BigDecimal calcFee(BigDecimal amount) {
        BigDecimal fee = amount.multiply(new BigDecimal("0.001"));
        if (fee.compareTo(new BigDecimal("100")) < 0) {
            return new BigDecimal("100");
        }
        if (fee.compareTo(new BigDecimal("5000")) > 0) {
            return new BigDecimal("5000");
        }
        return fee;
    }
}
