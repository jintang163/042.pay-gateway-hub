package com.payhub.channel.transfer;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class BankTransfer implements TransferService {

    @Override
    public TransferResult transfer(TransferRequest request) {
        log.info("[UNION_PAY]开始执行银行转账, transferNo={}, bankName={}, bankCode={}, receiverAccount={}, receiverName={}, amount={}分",
                request.getTransferNo(), request.getBankName(), request.getBankCode(),
                request.getReceiverAccount(), request.getReceiverName(), request.getAmount());

        String channelTransferNo = "BK" + IdUtil.getSnowflakeNextIdStr();

        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        double random = Math.random();
        if (random < 0.18) {
            log.warn("[UNION_PAY]银行转账失败, transferNo={}, reason=联行号错误或开户行不支持", request.getTransferNo());
            return TransferResult.fail(request.getTransferNo(), "银行联行号错误或开户行不支持");
        } else if (random < 0.25) {
            log.info("[UNION_PAY]银行转账处理中, transferNo={}, channelTransferNo={}", request.getTransferNo(), channelTransferNo);
            return TransferResult.processing(request.getTransferNo(), channelTransferNo);
        } else {
            log.info("[UNION_PAY]银行转账成功, transferNo={}, channelTransferNo={}, amount={}分",
                    request.getTransferNo(), channelTransferNo, request.getAmount());
            return TransferResult.success(request.getTransferNo(), channelTransferNo, LocalDateTime.now());
        }
    }

    @Override
    public String getChannelCode() {
        return "UNION_PAY";
    }
}
