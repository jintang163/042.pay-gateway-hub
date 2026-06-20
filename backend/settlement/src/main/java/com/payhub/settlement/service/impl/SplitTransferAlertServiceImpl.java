package com.payhub.settlement.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.payhub.common.utils.EmailUtil;
import com.payhub.common.utils.SmsUtil;
import com.payhub.settlement.entity.PaySplitDetail;
import com.payhub.settlement.service.SplitTransferAlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class SplitTransferAlertServiceImpl implements SplitTransferAlertService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired(required = false)
    private EmailUtil emailUtil;

    @Value("${payhub.split.transfer.alert.enabled:true}")
    private boolean alertEnabled;

    @Value("${payhub.split.transfer.alert.emails:}")
    private String alertEmails;

    @Value("${payhub.split.transfer.alert.phones:}")
    private String alertPhones;

    @Value("${payhub.split.transfer.alert.retry-exhausted-threshold:5}")
    private int retryExhaustedThreshold;

    @Override
    public void alertTransferFailed(PaySplitDetail detail) {
        if (!alertEnabled || detail == null) {
            return;
        }
        if (detail.getTransferRetryCount() != null
                && detail.getTransferRetryCount() < retryExhaustedThreshold
                && detail.getTransferRetryCount() > 1
                && detail.getTransferRetryCount() % 2 != 0) {
            return;
        }

        String subject = "[告警] 分账代付失败";
        String content = buildFailedContent(detail);
        sendAlert(subject, content);
        log.warn("分账代付失败告警已发送: splitDetailNo={}, retryCount={}",
                detail.getSplitDetailNo(), detail.getTransferRetryCount());
    }

    @Override
    public void alertTransferRetryExhausted(PaySplitDetail detail) {
        if (!alertEnabled || detail == null) {
            return;
        }
        String subject = "[严重告警] 分账代付重试次数耗尽";
        String content = buildExhaustedContent(detail);
        sendAlert(subject, content);
        log.error("分账代付重试耗尽告警已发送: splitDetailNo={}, retryCount={}",
                detail.getSplitDetailNo(), detail.getTransferRetryCount());
    }

    @Override
    public void alertBatchTransferFailed(List<PaySplitDetail> details) {
        if (!alertEnabled || CollUtil.isEmpty(details)) {
            return;
        }
        String subject = "[告警] 批量分账代付失败";
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>批量分账代付失败告警</h3>");
        sb.append("<p>失败数量: ").append(details.size()).append("</p>");
        sb.append("<table border='1' cellpadding='5' cellspacing='0'>");
        sb.append("<tr><th>分账明细号</th><th>订单号</th><th>商户号</th><th>接收方</th><th>金额(分)</th><th>失败原因</th><th>重试次数</th></tr>");
        for (PaySplitDetail d : details) {
            sb.append("<tr>");
            sb.append("<td>").append(d.getSplitDetailNo()).append("</td>");
            sb.append("<td>").append(d.getOrderNo()).append("</td>");
            sb.append("<td>").append(d.getMerchantNo()).append("</td>");
            sb.append("<td>").append(d.getReceiverName() != null ? d.getReceiverName() : d.getReceiverAccount()).append("</td>");
            sb.append("<td>").append(d.getSplitAmount() != null ? d.getSplitAmount() : BigDecimal.ZERO).append("</td>");
            sb.append("<td>").append(d.getTransferFailReason() != null ? d.getTransferFailReason() : "-").append("</td>");
            sb.append("<td>").append(d.getTransferRetryCount() != null ? d.getTransferRetryCount() : 0).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");
        sendAlert(subject, sb.toString());
        log.warn("批量分账代付失败告警已发送: failCount={}", details.size());
    }

    private String buildFailedContent(PaySplitDetail detail) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>分账代付失败告警</h3>");
        sb.append("<p><b>分账明细号:</b> ").append(detail.getSplitDetailNo()).append("</p>");
        sb.append("<p><b>订单号:</b> ").append(detail.getOrderNo()).append("</p>");
        sb.append("<p><b>商户号:</b> ").append(detail.getMerchantNo()).append("</p>");
        sb.append("<p><b>接收方:</b> ").append(detail.getReceiverName() != null ? detail.getReceiverName() : "-").append(" (").append(detail.getReceiverAccount()).append(")</p>");
        sb.append("<p><b>分账金额:</b> ").append(detail.getSplitAmount() != null ? detail.getSplitAmount() : BigDecimal.ZERO).append(" 分</p>");
        sb.append("<p><b>代付通道:</b> ").append(detail.getTransferChannel() != null ? detail.getTransferChannel() : "-").append("</p>");
        sb.append("<p><b>失败原因:</b> ").append(detail.getTransferFailReason() != null ? detail.getTransferFailReason() : "-").append("</p>");
        sb.append("<p><b>重试次数:</b> ").append(detail.getTransferRetryCount() != null ? detail.getTransferRetryCount() : 0).append("</p>");
        if (detail.getTransferTime() != null) {
            sb.append("<p><b>代付时间:</b> ").append(detail.getTransferTime().format(FORMATTER)).append("</p>");
        }
        return sb.toString();
    }

    private String buildExhaustedContent(PaySplitDetail detail) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3 style='color:red;'>分账代付重试次数耗尽 - 需人工介入</h3>");
        sb.append("<p><b>分账明细号:</b> ").append(detail.getSplitDetailNo()).append("</p>");
        sb.append("<p><b>订单号:</b> ").append(detail.getOrderNo()).append("</p>");
        sb.append("<p><b>商户号:</b> ").append(detail.getMerchantNo()).append("</p>");
        sb.append("<p><b>接收方:</b> ").append(detail.getReceiverName() != null ? detail.getReceiverName() : "-").append(" (").append(detail.getReceiverAccount()).append(")</p>");
        sb.append("<p><b>分账金额:</b> ").append(detail.getSplitAmount() != null ? detail.getSplitAmount() : BigDecimal.ZERO).append(" 分</p>");
        sb.append("<p><b>代付通道:</b> ").append(detail.getTransferChannel() != null ? detail.getTransferChannel() : "-").append("</p>");
        sb.append("<p><b>最后失败原因:</b> ").append(detail.getTransferFailReason() != null ? detail.getTransferFailReason() : "-").append("</p>");
        sb.append("<p><b>已重试次数:</b> ").append(detail.getTransferRetryCount() != null ? detail.getTransferRetryCount() : 0).append("</p>");
        sb.append("<p style='color:red;'><b>请及时人工处理该笔代付！</b></p>");
        return sb.toString();
    }

    private void sendAlert(String subject, String content) {
        if (StrUtil.isNotBlank(alertEmails) && emailUtil != null) {
            try {
                emailUtil.sendSimpleEmail(alertEmails, subject, content);
            } catch (Exception e) {
                log.error("发送分账代付告警邮件失败", e);
            }
        }
        if (StrUtil.isNotBlank(alertPhones)) {
            try {
                String[] phones = alertPhones.split("[,;，；\\s]+");
                for (String phone : phones) {
                    if (StrUtil.isNotBlank(phone)) {
                        SmsUtil.sendSms(phone.trim(), subject);
                    }
                }
            } catch (Exception e) {
                log.error("发送分账代付告警短信失败", e);
            }
        }
    }
}
