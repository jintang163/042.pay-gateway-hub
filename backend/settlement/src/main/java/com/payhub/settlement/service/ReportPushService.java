package com.payhub.settlement.service;

public interface ReportPushService {

    void manualPush(Long id);

    void triggerDailyReportPush();

    void triggerWeeklyReportPush();

    void dispatchScheduledPushes();
}
