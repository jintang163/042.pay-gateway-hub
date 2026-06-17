package com.payhub.admin.config;

import com.payhub.pay.service.PayRefundService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
public class ScheduleConfig {

    @Autowired
    private PayRefundService payRefundService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void refundRetryTask() {
        log.info("定时任务：开始执行退款重试任务");
        try {
            payRefundService.retryRefund();
            log.info("定时任务：退款重试任务执行完成");
        } catch (Exception e) {
            log.error("定时任务：退款重试任务执行异常", e);
        }
    }
}
