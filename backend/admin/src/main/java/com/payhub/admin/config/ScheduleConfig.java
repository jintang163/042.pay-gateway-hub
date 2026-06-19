package com.payhub.admin.config;

import com.payhub.common.sandbox.SandboxDataCleanService;
import com.payhub.pay.service.PayRefundService;
import com.payhub.settlement.service.ReportPushService;
import com.payhub.settlement.service.SettlementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;

@Slf4j
@Configuration
@EnableScheduling
public class ScheduleConfig {

    @Autowired
    private PayRefundService payRefundService;

    @Autowired
    private SettlementService settlementService;

    @Autowired(required = false)
    private SandboxDataCleanService sandboxDataCleanService;

    @Autowired
    private ReportPushService reportPushService;

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

    @Scheduled(cron = "0 0 2 * * ?")
    public void generateSettlementTask() {
        log.info("定时任务：开始执行结算生成任务");
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            settlementService.generateSettlement(yesterday);
            log.info("定时任务：结算生成任务执行完成, 结算日期: {}", yesterday);
        } catch (Exception e) {
            log.error("定时任务：结算生成任务执行异常", e);
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void executeSettlementTask() {
        log.info("定时任务：开始执行批量结算打款任务");
        try {
            settlementService.executeSettlementTask();
            log.info("定时任务：批量结算打款任务执行完成");
        } catch (Exception e) {
            log.error("定时任务：批量结算打款任务执行异常", e);
        }
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanSandboxDataTask() {
        log.info("定时任务：开始执行沙箱数据清理任务");
        try {
            if (sandboxDataCleanService != null) {
                sandboxDataCleanService.cleanAllSandboxData();
                log.info("定时任务：沙箱数据清理任务执行完成");
            } else {
                log.info("定时任务：沙箱数据清理服务未启用，跳过");
            }
        } catch (Exception e) {
            log.error("定时任务：沙箱数据清理任务执行异常", e);
        }
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void dailyReportPushTask() {
        log.info("定时任务：开始执行日报推送任务");
        try {
            reportPushService.triggerDailyReportPush();
            log.info("定时任务：日报推送任务执行完成");
        } catch (Exception e) {
            log.error("定时任务：日报推送任务执行异常", e);
        }
    }

    @Scheduled(cron = "0 0 9 ? * MON")
    public void weeklyReportPushTask() {
        log.info("定时任务：开始执行周报推送任务");
        try {
            reportPushService.triggerWeeklyReportPush();
            log.info("定时任务：周报推送任务执行完成");
        } catch (Exception e) {
            log.error("定时任务：周报推送任务执行异常", e);
        }
    }
}
