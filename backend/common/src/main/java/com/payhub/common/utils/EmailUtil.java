package com.payhub.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailUtil {

    public boolean sendEmailWithAttachment(String to, String subject, String content,
                                            String attachmentFileName, byte[] attachmentData) {
        log.info("========== 【沙箱模式】模拟发送带附件邮件 ==========");
        log.info("收件人: {}", to);
        if (to != null && to.contains(",")) {
            String[] recipients = to.split(",");
            log.info("解析收件人数量: {} 个", recipients.length);
            for (int i = 0; i < recipients.length; i++) {
                log.info("  收件人{}: {}", i + 1, recipients[i].trim());
            }
        }
        log.info("邮件主题: {}", subject);
        log.info("邮件正文: {}", content);
        log.info("附件文件名: {}", attachmentFileName);
        log.info("附件数据大小: {} bytes", attachmentData != null ? attachmentData.length : 0);
        log.info("========== 邮件模拟发送完成（返回true） ==========");
        return true;
    }

    public boolean sendSimpleEmail(String to, String subject, String content) {
        log.info("========== 【沙箱模式】模拟发送纯文本邮件 ==========");
        log.info("收件人: {}", to);
        if (to != null && to.contains(",")) {
            String[] recipients = to.split(",");
            log.info("解析收件人数量: {} 个", recipients.length);
            for (int i = 0; i < recipients.length; i++) {
                log.info("  收件人{}: {}", i + 1, recipients[i].trim());
            }
        }
        log.info("邮件主题: {}", subject);
        log.info("邮件正文: {}", content);
        log.info("========== 邮件模拟发送完成（返回true） ==========");
        return true;
    }
}
