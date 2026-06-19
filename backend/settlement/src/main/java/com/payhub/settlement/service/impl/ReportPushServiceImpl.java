package com.payhub.settlement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import com.payhub.common.utils.EmailUtil;
import com.payhub.common.utils.OrderNoGenerator;
import com.payhub.merchant.entity.MerchantInfo;
import com.payhub.merchant.service.MerchantInfoService;
import com.payhub.settlement.entity.ReportPushRecord;
import com.payhub.settlement.entity.ReportSubscription;
import com.payhub.settlement.entity.SettlementRecord;
import com.payhub.settlement.mapper.ReportPushRecordMapper;
import com.payhub.settlement.mapper.SettlementRecordMapper;
import com.payhub.settlement.service.PdfReportService;
import com.payhub.settlement.service.ReportPushRecordService;
import com.payhub.settlement.service.ReportPushService;
import com.payhub.settlement.service.ReportSubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class ReportPushServiceImpl implements ReportPushService {

    @Autowired
    private ReportSubscriptionService reportSubscriptionService;

    @Autowired
    private ReportPushRecordService reportPushRecordService;

    @Autowired
    private ReportPushRecordMapper reportPushRecordMapper;

    @Autowired
    private MerchantInfoService merchantInfoService;

    @Autowired
    private SettlementRecordMapper settlementRecordMapper;

    @Autowired
    private PdfReportService pdfReportService;

    @Autowired
    private EmailUtil emailUtil;

    private static final int PUSH_STATUS_PENDING = 0;
    private static final int PUSH_STATUS_PROCESSING = 1;
    private static final int PUSH_STATUS_SUCCESS = 2;
    private static final int PUSH_STATUS_FAIL = 3;

    private static final int SETTLE_STATUS_PENDING = 0;
    private static final int SETTLE_STATUS_PROCESSING = 1;
    private static final int SETTLE_STATUS_SUCCESS = 2;
    private static final int SETTLE_STATUS_FAIL = 3;

    private static final int TRIGGER_SCHEDULED = 1;
    private static final int TRIGGER_MANUAL = 2;

    private static final int REPORT_TYPE_DAILY = 1;
    private static final int REPORT_TYPE_WEEKLY = 2;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HM_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manualPush(Long id) {
        ReportSubscription subscription = reportSubscriptionService.getById(id);
        if (subscription == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "报表订阅不存在");
        }
        log.info("手动推送报表: subscriptionId={}, subscriptionNo={}, merchantNo={}",
                id, subscription.getSubscriptionNo(), subscription.getMerchantNo());
        processPush(subscription, TRIGGER_MANUAL);
    }

    @Override
    public void dispatchScheduledPushes() {
        LocalDateTime now = LocalDateTime.now();
        String currentHm = now.format(HM_FMT);
        LocalDate today = now.toLocalDate();

        log.debug("开始扫描报表订阅调度: 当前时间={}", currentHm);

        LambdaQueryWrapper<ReportSubscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportSubscription::getEnabled, 1);
        List<ReportSubscription> subscriptions = reportSubscriptionService.list(wrapper);

        int triggered = 0;
        int skipped = 0;

        for (ReportSubscription sub : subscriptions) {
            try {
                if (!isTimeToPush(sub, currentHm, today)) {
                    skipped++;
                    continue;
                }

                LocalDate endDate = today.minusDays(1);
                LocalDate startDate;
                if (REPORT_TYPE_DAILY == sub.getReportType()) {
                    startDate = endDate;
                } else if (REPORT_TYPE_WEEKLY == sub.getReportType()) {
                    startDate = endDate.minusDays(6);
                } else {
                    continue;
                }

                if (!isSettlementCompleted(sub.getMerchantNo(), startDate, endDate)) {
                    log.info("订阅[{}]商户[{}]报表周期[{}~{}]结算未全部打款完成，暂不推送",
                            sub.getSubscriptionNo(), sub.getMerchantNo(), startDate, endDate);
                    skipped++;
                    continue;
                }

                if (hasPushedForPeriod(sub.getSubscriptionNo(), startDate, endDate)) {
                    log.debug("订阅[{}]商户[{}]周期[{}~{}]已推送过，跳过重复推送",
                            sub.getSubscriptionNo(), sub.getMerchantNo(), startDate, endDate);
                    skipped++;
                    continue;
                }

                log.info("调度触发推送: subscriptionNo={}, merchantNo={}, reportType={}, 周期={}~{}",
                        sub.getSubscriptionNo(), sub.getMerchantNo(),
                        REPORT_TYPE_DAILY == sub.getReportType() ? "日报" : "周报",
                        startDate, endDate);

                processPush(sub, TRIGGER_SCHEDULED);
                triggered++;
            } catch (Exception e) {
                log.error("调度推送异常: subscriptionNo={}, merchantNo={}, error={}",
                        sub.getSubscriptionNo(), sub.getMerchantNo(), e.getMessage(), e);
            }
        }

        if (triggered > 0 || skipped > 0) {
            log.debug("报表订阅调度扫描完成: 触发={}, 跳过={}, 总计={}", triggered, skipped, subscriptions.size());
        }
    }

    private boolean isTimeToPush(ReportSubscription sub, String currentHm, LocalDate today) {
        if (sub.getPushTime() == null || sub.getPushTime().trim().isEmpty()) {
            return false;
        }
        String subHm = sub.getPushTime().trim();
        if (!subHm.equals(currentHm)) {
            return false;
        }
        if (REPORT_TYPE_DAILY == sub.getReportType()) {
            return true;
        }
        if (REPORT_TYPE_WEEKLY == sub.getReportType()) {
            return DayOfWeek.MONDAY.equals(today.getDayOfWeek());
        }
        return false;
    }

    private boolean isSettlementCompleted(String merchantNo, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<SettlementRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SettlementRecord::getMerchantNo, merchantNo)
                .between(SettlementRecord::getSettleDate, startDate, endDate)
                .in(SettlementRecord::getSettleStatus, SETTLE_STATUS_PENDING, SETTLE_STATUS_PROCESSING);
        Long pendingCount = settlementRecordMapper.selectCount(wrapper);
        boolean completed = pendingCount == null || pendingCount == 0;
        log.debug("商户[{}]周期[{}~{}]结算完成检查: pendingCount={}, completed={}",
                merchantNo, startDate, endDate, pendingCount, completed);
        return completed;
    }

    private boolean hasPushedForPeriod(String subscriptionNo, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<ReportPushRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportPushRecord::getSubscriptionNo, subscriptionNo)
                .eq(ReportPushRecord::getStartDate, startDate)
                .eq(ReportPushRecord::getEndDate, endDate)
                .eq(ReportPushRecord::getTriggerType, TRIGGER_SCHEDULED)
                .in(ReportPushRecord::getPushStatus, PUSH_STATUS_PROCESSING, PUSH_STATUS_SUCCESS);
        Long count = reportPushRecordMapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerDailyReportPush() {
        log.info("========== 开始执行日报推送任务（批量触发） ==========");
        LambdaQueryWrapper<ReportSubscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportSubscription::getReportType, REPORT_TYPE_DAILY)
                .eq(ReportSubscription::getEnabled, 1);
        List<ReportSubscription> subscriptions = reportSubscriptionService.list(wrapper);
        log.info("查询到启用的日报订阅数量: {}", subscriptions.size());

        int successCount = 0;
        int failCount = 0;
        for (ReportSubscription subscription : subscriptions) {
            try {
                processPush(subscription, TRIGGER_SCHEDULED);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("日报推送失败: subscriptionId={}, merchantNo={}, error={}",
                        subscription.getId(), subscription.getMerchantNo(), e.getMessage(), e);
            }
        }
        log.info("========== 日报推送任务完成: 成功={}, 失败={}, 总计={} ==========",
                successCount, failCount, subscriptions.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerWeeklyReportPush() {
        log.info("========== 开始执行周报推送任务（批量触发） ==========");
        LambdaQueryWrapper<ReportSubscription> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportSubscription::getReportType, REPORT_TYPE_WEEKLY)
                .eq(ReportSubscription::getEnabled, 1);
        List<ReportSubscription> subscriptions = reportSubscriptionService.list(wrapper);
        log.info("查询到启用的周报订阅数量: {}", subscriptions.size());

        int successCount = 0;
        int failCount = 0;
        for (ReportSubscription subscription : subscriptions) {
            try {
                processPush(subscription, TRIGGER_SCHEDULED);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("周报推送失败: subscriptionId={}, merchantNo={}, error={}",
                        subscription.getId(), subscription.getMerchantNo(), e.getMessage(), e);
            }
        }
        log.info("========== 周报推送任务完成: 成功={}, 失败={}, 总计={} ==========",
                successCount, failCount, subscriptions.size());
    }

    private void processPush(ReportSubscription subscription, Integer triggerType) {
        log.info("开始处理报表推送: subscriptionNo={}, merchantNo={}, reportType={}",
                subscription.getSubscriptionNo(), subscription.getMerchantNo(),
                REPORT_TYPE_DAILY == subscription.getReportType() ? "日报" : (REPORT_TYPE_WEEKLY == subscription.getReportType() ? "周报" : "未知"));

        ReportPushRecord record = new ReportPushRecord();
        record.setRecordNo(OrderNoGenerator.generateWithPrefix("RP"));
        record.setSubscriptionNo(subscription.getSubscriptionNo());
        record.setMerchantNo(subscription.getMerchantNo());
        record.setReportType(subscription.getReportType());
        record.setReportCategory(subscription.getReportCategory() != null ? subscription.getReportCategory() : "SETTLEMENT");
        record.setPushChannel(subscription.getPushChannel());
        record.setEmailTargets(subscription.getEmailList());
        record.setPhoneTargets(subscription.getPhoneList());
        record.setTriggerType(triggerType);
        record.setPushStatus(PUSH_STATUS_PENDING);
        record.setSuccessCount(0);
        record.setFailCount(0);

        LocalDate endDate = LocalDate.now().minusDays(1);
        LocalDate startDate;
        String reportTypeText;
        if (REPORT_TYPE_DAILY == subscription.getReportType()) {
            startDate = endDate;
            reportTypeText = "日报";
            record.setReportTitle(endDate.toString() + " 结算日报");
            record.setReportPeriod(endDate.toString());
        } else if (REPORT_TYPE_WEEKLY == subscription.getReportType()) {
            startDate = endDate.minusDays(6);
            reportTypeText = "周报";
            record.setReportTitle(startDate.toString() + " 至 " + endDate.toString() + " 结算周报");
            record.setReportPeriod(startDate.toString() + " ~ " + endDate.toString());
        } else {
            startDate = endDate;
            reportTypeText = "报表";
            record.setReportTitle(endDate.toString() + " 结算报表");
            record.setReportPeriod(endDate.toString());
        }
        record.setStartDate(startDate);
        record.setEndDate(endDate);
        reportPushRecordMapper.insert(record);
        log.info("已创建推送记录: recordId={}, recordNo={}, 日期范围={} ~ {}",
                record.getId(), record.getRecordNo(), startDate, endDate);

        try {
            record.setPushStatus(PUSH_STATUS_PROCESSING);
            reportPushRecordMapper.updateById(record);

            LambdaQueryWrapper<SettlementRecord> settleWrapper = new LambdaQueryWrapper<>();
            settleWrapper.eq(SettlementRecord::getMerchantNo, subscription.getMerchantNo())
                    .between(SettlementRecord::getSettleDate, startDate, endDate)
                    .orderByDesc(SettlementRecord::getSettleDate);
            List<SettlementRecord> settlementRecords = settlementRecordMapper.selectList(settleWrapper);
            log.info("查询到结算记录数: merchantNo={}, count={}", subscription.getMerchantNo(), settlementRecords.size());

            String merchantName = getMerchantName(subscription.getMerchantNo());
            byte[] pdfData = pdfReportService.generateSettlementPdf(
                    subscription.getMerchantNo(),
                    merchantName,
                    startDate,
                    endDate,
                    settlementRecords
            );
            String fileName = String.format("结算报表_%s_%s-%s.pdf",
                    subscription.getMerchantNo(),
                    startDate.format(DATE_FMT),
                    endDate.format(DATE_FMT));
            record.setFileSize((long) pdfData.length);

            String emailSubject = String.format("【支付网关】商户%s结算%s - %s",
                    merchantName != null ? merchantName : subscription.getMerchantNo(),
                    reportTypeText,
                    record.getReportPeriod());

            StringBuilder emailContent = new StringBuilder();
            emailContent.append("<html><body style='font-family:Microsoft YaHei,Arial,sans-serif;padding:30px;'>");
            emailContent.append("<div style='max-width:700px;margin:0 auto;'>");
            emailContent.append("<div style='background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);padding:25px;border-radius:8px 8px 0 0;color:#fff;'>");
            emailContent.append("<h2 style='margin:0;font-size:22px;'>商户结算报表</h2>");
            emailContent.append("<p style='margin:8px 0 0 0;opacity:0.9;'>Settlement Report</p>");
            emailContent.append("</div>");
            emailContent.append("<div style='background:#fff;padding:30px;border:1px solid #e8e8e8;border-top:none;'>");
            emailContent.append("<p style='font-size:16px;color:#333;line-height:1.8;'>尊敬的商户 <strong>").append(merchantName != null ? merchantName : subscription.getMerchantNo()).append("</strong>, 您好：</p>");
            emailContent.append("<p style='font-size:14px;color:#555;line-height:2;margin-top:15px;'>");
            emailContent.append("附件为您的结算").append(reportTypeText).append("，报表周期为 <strong>").append(record.getReportPeriod()).append("</strong>。<br>");
            emailContent.append("请查收附件，如有疑问请联系商户服务中心。</p>");
            emailContent.append("<div style='background:#f8fafd;border:1px solid #dce8f5;border-radius:6px;padding:20px;margin-top:25px;'>");
            emailContent.append("<div style='display:grid;grid-template-columns:1fr 1fr;gap:12px;font-size:13px;color:#666;'>");
            emailContent.append("<div><strong>商户编号：</strong>").append(subscription.getMerchantNo()).append("</div>");
            emailContent.append("<div><strong>报表类型：</strong>").append(reportTypeText).append("</div>");
            emailContent.append("<div><strong>结算笔数：</strong>").append(settlementRecords.size()).append(" 笔</div>");
            emailContent.append("<div><strong>附件大小：</strong>").append(String.format("%.2f KB", pdfData.length / 1024.0)).append("</div>");
            emailContent.append("</div></div>");
            emailContent.append("<p style='font-size:12px;color:#999;margin-top:25px;'>— 本邮件由支付网关聚合平台自动发送，请勿直接回复 —</p>");
            emailContent.append("</div></div></body></html>");

            String[] emails = subscription.getEmailList().split("[,;，；\\s]+");
            int emailSuccessCount = 0;
            int emailFailCount = 0;
            StringBuilder failReasons = new StringBuilder();
            for (String email : emails) {
                if (email == null || email.trim().isEmpty()) continue;
                try {
                    boolean ok = emailUtil.sendEmailWithAttachment(
                            email.trim(),
                            emailSubject,
                            emailContent.toString(),
                            fileName,
                            pdfData
                    );
                    if (ok) {
                        emailSuccessCount++;
                    } else {
                        emailFailCount++;
                        failReasons.append(email).append(":发送失败; ");
                    }
                } catch (Exception e) {
                    emailFailCount++;
                    failReasons.append(email).append(":").append(e.getMessage()).append("; ");
                }
            }

            if (subscription.getPhoneList() != null && !subscription.getPhoneList().trim().isEmpty()
                    && subscription.getPushChannel() != null && subscription.getPushChannel() == 2) {
                String[] phones = subscription.getPhoneList().split("[,;，；\\s]+");
                for (String phone : phones) {
                    if (phone == null || phone.trim().isEmpty()) continue;
                    log.info("模拟发送短信通知: phone={}, 内容={}", phone.trim(), emailSubject);
                }
            }

            record.setSuccessCount(emailSuccessCount);
            record.setFailCount(emailFailCount);
            record.setPushTime(LocalDateTime.now());

            if (emailSuccessCount > 0 && emailFailCount == 0) {
                record.setPushStatus(PUSH_STATUS_SUCCESS);
                log.info("报表推送成功: recordId={}, 成功邮箱={}个", record.getId(), emailSuccessCount);
            } else if (emailSuccessCount > 0) {
                record.setPushStatus(PUSH_STATUS_SUCCESS);
                record.setFailReason("部分邮箱推送失败: " + failReasons.toString());
                log.warn("报表部分推送成功: recordId={}, 成功={}, 失败={}", record.getId(), emailSuccessCount, emailFailCount);
            } else {
                record.setPushStatus(PUSH_STATUS_FAIL);
                record.setFailReason("所有邮箱推送失败: " + (failReasons.length() > 0 ? failReasons.toString() : "未知原因"));
                log.error("报表推送全部失败: recordId={}, reason={}", record.getId(), record.getFailReason());
            }
            reportPushRecordMapper.updateById(record);

        } catch (Exception e) {
            log.error("报表推送异常: recordId={}, merchantNo={}", record.getId(), subscription.getMerchantNo(), e);
            record.setPushStatus(PUSH_STATUS_FAIL);
            record.setFailCount(1);
            record.setFailReason("推送异常: " + e.getMessage());
            record.setPushTime(LocalDateTime.now());
            reportPushRecordMapper.updateById(record);
            throw new RuntimeException("报表推送失败: " + e.getMessage(), e);
        }
    }

    private String getMerchantName(String merchantNo) {
        try {
            LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MerchantInfo::getMerchantNo, merchantNo);
            MerchantInfo info = merchantInfoService.getOne(wrapper);
            return info != null ? info.getMerchantName() : null;
        } catch (Exception e) {
            log.warn("获取商户名称失败: merchantNo={}", merchantNo, e);
            return null;
        }
    }
}
