package com.payhub.merchant.job;

import com.payhub.merchant.service.FeePromotionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FeePromotionJob {

    @Autowired
    private FeePromotionService feePromotionService;

    @Scheduled(cron = "0 */1 * * * ?")
    public void refreshPromotionStatus() {
        try {
            feePromotionService.refreshPromotionStatus();
        } catch (Exception e) {
            log.error("活动状态刷新任务异常", e);
        }
    }
}
