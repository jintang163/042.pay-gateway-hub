package com.payhub.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;

@Slf4j
@Component
public class EmailUtil {

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    @Value("${payhub.mail.sandbox:true}")
    private boolean sandbox;

    @Value("${payhub.mail.sender-name:支付网关聚合平台}")
    private String senderName;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    public boolean sendEmailWithAttachment(String to, String subject, String content,
                                            String attachmentFileName, byte[] attachmentData) {
        log.info("========== 开始发送带附件邮件 ==========");
        log.info("收件人: {}, 主题: {}, 附件: {}, sandbox模式: {}", to, subject, attachmentFileName, sandbox);

        if (sandbox) {
            return mockSendEmail(to, subject, content, attachmentFileName, attachmentData);
        }

        try {
            MimeMessage message = buildMimeMessage(to, subject, content, true, attachmentFileName, attachmentData);
            javaMailSender.send(message);
            log.info("========== 带附件邮件真实发送成功 ==========");
            return true;
        } catch (Exception e) {
            log.error("带附件邮件真实发送失败: to={}, subject={}, error={}", to, subject, e.getMessage(), e);
            return false;
        }
    }

    public boolean sendSimpleEmail(String to, String subject, String content) {
        log.info("========== 开始发送纯文本/HTML邮件 ==========");
        log.info("收件人: {}, 主题: {}, sandbox模式: {}", to, subject, sandbox);

        if (sandbox) {
            return mockSendEmail(to, subject, content, null, null);
        }

        try {
            MimeMessage message = buildMimeMessage(to, subject, content, false, null, null);
            javaMailSender.send(message);
            log.info("========== 邮件真实发送成功 ==========");
            return true;
        } catch (Exception e) {
            log.error("邮件真实发送失败: to={}, subject={}, error={}", to, subject, e.getMessage(), e);
            return false;
        }
    }

    private MimeMessage buildMimeMessage(String to, String subject, String content,
                                          boolean hasAttachment, String attachmentFileName,
                                          byte[] attachmentData) throws Exception {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, hasAttachment, "UTF-8");

        helper.setFrom(buildFromAddress());
        helper.setTo(to.split("[,;，；\\s]+"));
        helper.setSubject(subject);
        helper.setText(content, true);

        if (hasAttachment && attachmentFileName != null && attachmentData != null) {
            String encodedFileName = MimeUtility.encodeText(attachmentFileName, "UTF-8", "B");
            helper.addAttachment(encodedFileName, new ByteArrayResource(attachmentData) {
                @Override
                public String getFilename() {
                    return encodedFileName;
                }
            });
        }
        return message;
    }

    private InternetAddress buildFromAddress() throws UnsupportedEncodingException {
        String personal = senderName != null ? senderName : "支付网关聚合平台";
        if (senderEmail == null || senderEmail.isEmpty()) {
            return new InternetAddress("no-reply@payhub.com", personal, "UTF-8");
        }
        return new InternetAddress(senderEmail, personal, "UTF-8");
    }

    private boolean mockSendEmail(String to, String subject, String content,
                                   String attachmentFileName, byte[] attachmentData) {
        log.info("========== 【沙箱模式】模拟发送邮件 ==========");
        log.info("发件人: {} <{}>", senderName, senderEmail != null && !senderEmail.isEmpty() ? senderEmail : "no-reply@payhub.com");
        if (to != null && to.contains(",")) {
            String[] recipients = to.split("[,;，；\\s]+");
            log.info("解析收件人数量: {} 个", recipients.length);
            for (int i = 0; i < recipients.length; i++) {
                log.info("  收件人{}: {}", i + 1, recipients[i].trim());
            }
        } else {
            log.info("收件人: {}", to);
        }
        log.info("邮件主题: {}", subject);
        log.info("邮件正文长度: {} 字符", content != null ? content.length() : 0);
        if (attachmentFileName != null) {
            log.info("附件文件名: {}", attachmentFileName);
        }
        if (attachmentData != null) {
            log.info("附件数据大小: {} bytes ({} KB)", attachmentData.length, String.format("%.2f", attachmentData.length / 1024.0));
        }
        log.info("========== 【沙箱模式】邮件模拟发送完成（返回true） ==========");
        return true;
    }
}
