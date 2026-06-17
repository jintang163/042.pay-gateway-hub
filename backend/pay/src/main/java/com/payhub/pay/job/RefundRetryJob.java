package com.payhub.pay.job;

import com.payhub.pay.service.PayRefundService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RefundRetryJob {

    @Autowired
    private PayRefundService payRefundService;

    @Scheduled(cron = "0 */1 * * * ?")
    public void retryRefund() {
        try {
            log.info("退款重试定时任务开始执行");
            payRefundService.retryRefund();
            log.info("退款重试定时任务执行完成");
        } catch (Exception e) {
            log.error("退款重试定时任务执行异常", e);
        }
    }
}
