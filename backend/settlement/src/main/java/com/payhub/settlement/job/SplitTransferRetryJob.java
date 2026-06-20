package com.payhub.settlement.job;

import com.payhub.settlement.service.SplitTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SplitTransferRetryJob {

    @Autowired(required = false)
    private SplitTransferService splitTransferService;

    @Value("${payhub.split.transfer.retry.batch-size:100}")
    private int batchSize;

    @Value("${payhub.split.transfer.retry.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "0 */5 * * * ?")
    public void retryPendingTransfers() {
        if (!enabled) {
            return;
        }
        if (splitTransferService == null) {
            log.warn("SplitTransferService 未注入，跳过分账代付重试任务");
            return;
        }
        try {
            log.info("开始执行分账代付自动重试任务, batchSize={}", batchSize);
            boolean result = splitTransferService.processPendingTransfers(batchSize);
            log.info("分账代付自动重试任务执行完成, result={}", result);
        } catch (Exception e) {
            log.error("分账代付自动重试任务执行异常", e);
        }
    }
}
