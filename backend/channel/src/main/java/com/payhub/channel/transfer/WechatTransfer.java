package com.payhub.channel.transfer;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class WechatTransfer implements TransferService {

    @Override
    public TransferResult transfer(TransferRequest request) {
        log.info("[WECHAT_PAY]开始执行微信转账, transferNo={}, receiverAccount={}, receiverName={}, amount={}分",
                request.getTransferNo(), request.getReceiverAccount(),
                request.getReceiverName(), request.getAmount());

        String channelTransferNo = "WX" + IdUtil.getSnowflakeNextIdStr();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        double random = Math.random();
        if (random < 0.15) {
            log.warn("[WECHAT_PAY]微信转账失败, transferNo={}, reason=收款账户异常", request.getTransferNo());
            return TransferResult.fail(request.getTransferNo(), "微信收款账户异常");
        } else if (random < 0.20) {
            log.info("[WECHAT_PAY]微信转账处理中, transferNo={}, channelTransferNo={}", request.getTransferNo(), channelTransferNo);
            return TransferResult.processing(request.getTransferNo(), channelTransferNo);
        } else {
            log.info("[WECHAT_PAY]微信转账成功, transferNo={}, channelTransferNo={}, amount={}分",
                    request.getTransferNo(), channelTransferNo, request.getAmount());
            return TransferResult.success(request.getTransferNo(), channelTransferNo, LocalDateTime.now());
        }
    }

    @Override
    public String getChannelCode() {
        return "WECHAT_PAY";
    }

    @Override
    public TransferResult query(String transferNo, String channelTransferNo) {
        log.info("[WECHAT_PAY]查询微信转账状态, transferNo={}, channelTransferNo={}", transferNo, channelTransferNo);
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
            return TransferResult.fail(transferNo, "E006", "微信查询结果：转账已退回");
        }
    }
}
