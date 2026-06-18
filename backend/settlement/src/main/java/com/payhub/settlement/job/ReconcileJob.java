package com.payhub.settlement.job;

import com.payhub.common.enums.PayChannelEnum;
import com.payhub.settlement.service.ReconcileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class ReconcileJob {

    @Autowired
    private ReconcileService reconcileService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyReconcile() {
        log.info("开始执行每日自动对账任务");
        LocalDate reconcileDate = LocalDate.now().minusDays(1);
        executeAllChannels(reconcileDate);
        log.info("每日自动对账任务执行完成");
    }

    private void executeAllChannels(LocalDate reconcileDate) {
        for (PayChannelEnum channel : PayChannelEnum.values()) {
            try {
                log.info("开始执行通道对账, 通道: {}, 日期: {}", channel.getCode(), reconcileDate);
                reconcileService.executeReconcile(channel.getCode(), reconcileDate);
                log.info("通道对账完成, 通道: {}, 日期: {}", channel.getCode(), reconcileDate);
            } catch (Exception e) {
                log.error("通道对账失败, 通道: {}, 日期: {}", channel.getCode(), reconcileDate, e);
            }
        }
    }
}
