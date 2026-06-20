package com.payhub.settlement.job;

import com.payhub.settlement.service.MerchantWithdrawService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MerchantWithdrawJob {

    @Autowired
    private MerchantWithdrawService merchantWithdrawService;

    @Value("${payhub.merchant.withdraw.t1-batch-size:100}")
    private int t1BatchSize;

    @Value("${payhub.merchant.withdraw.t1-batch-enabled:true}")
    private boolean t1BatchEnabled;

    @Value("${payhub.merchant.withdraw.retry-enabled:true}")
    private boolean retryEnabled;

    @Scheduled(cron = "${payhub.merchant.withdraw.t1-batch-cron:0 0 2 * * ?}")
    public void processT1WithdrawBatch() {
        if (!t1BatchEnabled) {
            log.info("T+1提现批量转账任务已禁用，跳过");
            return;
        }
        try {
            log.info("开始执行T+1提现批量转账定时任务");
            merchantWithdrawService.processT1Batch(t1BatchSize);
            log.info("T+1提现批量转账定时任务执行完成");
        } catch (Exception e) {
            log.error("T+1提现批量转账定时任务执行异常", e);
        }
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void retryFailedWithdraw() {
        if (!retryEnabled) {
            log.info("商户提现重试任务已禁用，跳过");
            return;
        }
        try {
            log.info("开始执行商户提现失败重试定时任务");
            merchantWithdrawService.retryFailedWithdraw();
            log.info("商户提现失败重试定时任务执行完成");
        } catch (Exception e) {
            log.error("商户提现失败重试定时任务执行异常", e);
        }
    }
}
