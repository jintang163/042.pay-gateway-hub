package com.payhub.invoice.strategy;

import com.alibaba.fastjson2.JSON;
import com.payhub.invoice.config.InvoiceProperties;
import com.payhub.invoice.entity.InvoiceCallbackLog;
import com.payhub.invoice.mapper.InvoiceCallbackLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class AbstractInvoiceChannel implements InvoiceChannelStrategy {

    @Autowired
    protected InvoiceProperties invoiceProperties;

    @Autowired
    private InvoiceCallbackLogMapper invoiceCallbackLogMapper;

    protected void saveCallbackLog(String invoiceNo, String channelCode, String channelInvoiceNo,
                                   String requestBody, String responseBody, String notifyStatus) {
        try {
            InvoiceCallbackLog logEntity = new InvoiceCallbackLog();
            logEntity.setInvoiceNo(invoiceNo);
            logEntity.setChannelCode(channelCode);
            logEntity.setChannelInvoiceNo(channelInvoiceNo);
            logEntity.setRequestBody(requestBody);
            logEntity.setResponseBody(responseBody);
            logEntity.setNotifyStatus(notifyStatus);
            invoiceCallbackLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.warn("保存发票回调日志失败: invoiceNo={}, error={}", invoiceNo, e.getMessage());
        }
    }

    protected String buildCallbackUrl(String channelCode) {
        String baseUrl = invoiceProperties.getCallbackBaseUrl();
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        return baseUrl + "api/invoice/notify/" + channelCode.toLowerCase();
    }

    protected <T> T parseJson(String json, Class<T> clazz) {
        try {
            return JSON.parseObject(json, clazz);
        } catch (Exception e) {
            log.warn("解析JSON失败: json={}, error={}", json, e.getMessage());
            return null;
        }
    }

    protected String toJson(Object obj) {
        try {
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            log.warn("序列化JSON失败: error={}", e.getMessage());
            return "";
        }
    }
}
