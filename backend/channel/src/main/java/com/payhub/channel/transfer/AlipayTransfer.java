package com.payhub.channel.transfer;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
public class AlipayTransfer implements TransferService {

    @Override
    public TransferResult transfer(TransferRequest request) {
        log.info("[ALIPAY]开始执行支付宝转账, transferNo={}, receiverAccount={}, receiverName={}, amount={}分",
                request.getTransferNo(), request.getReceiverAccount(),
                request.getReceiverName(), request.getAmount());

        String channelTransferNo = "ALI" + IdUtil.getSnowflakeNextIdStr();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        double random = Math.random();
        if (random < 0.15) {
            log.warn("[ALIPAY]支付宝转账失败, transferNo={}, reason=账户余额不足", request.getTransferNo());
            return TransferResult.fail(request.getTransferNo(), "支付宝账户余额不足");
        } else if (random < 0.20) {
            log.info("[ALIPAY]支付宝转账处理中, transferNo={}, channelTransferNo={}", request.getTransferNo(), channelTransferNo);
            return TransferResult.processing(request.getTransferNo(), channelTransferNo);
        } else {
            log.info("[ALIPAY]支付宝转账成功, transferNo={}, channelTransferNo={}, amount={}分",
                    request.getTransferNo(), channelTransferNo, request.getAmount());
            return TransferResult.success(request.getTransferNo(), channelTransferNo, LocalDateTime.now());
        }
    }

    @Override
    public String getChannelCode() {
        return "ALIPAY";
    }

    @Override
    public TransferResult query(String transferNo, String channelTransferNo) {
        log.info("[ALIPAY]查询支付宝转账状态, transferNo={}, channelTransferNo={}", transferNo, channelTransferNo);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        double random = Math.random();
        if (random < 0.8) {
            return TransferResult.success(transferNo, channelTransferNo, LocalDateTime.now());
        } else if (random < 0.95) {
            return TransferResult.processing(transferNo, channelTransferNo);
        } else {
            return TransferResult.fail(transferNo, "E005", "支付宝查询结果：账户不存在或已冻结");
        }
    }
}
