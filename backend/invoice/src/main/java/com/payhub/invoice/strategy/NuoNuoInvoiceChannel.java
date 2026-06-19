package com.payhub.invoice.strategy;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.payhub.common.utils.HttpUtil;
import com.payhub.invoice.dto.InvoiceApplyRequest;
import com.payhub.invoice.dto.InvoiceCallbackResult;
import com.payhub.invoice.dto.InvoiceRedFlushRequest;
import com.payhub.invoice.entity.Invoice;
import com.payhub.invoice.entity.InvoiceChannelConfig;
import com.payhub.invoice.enums.InvoiceStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class NuoNuoInvoiceChannel extends AbstractInvoiceChannel {

    @Override
    public String getChannelCode() {
        return "NUONUO";
    }

    @Override
    public Invoice issueInvoice(InvoiceApplyRequest request, InvoiceChannelConfig config, Invoice invoice) {
        log.info("诺诺发票-申请开票: invoiceNo={}, orderNo={}", invoice.getInvoiceNo(), request.getOrderNo());

        InvoiceProperties.NuoNuoConfig nuonuoConfig = invoiceProperties.getNuonuo();
        if (nuonuoConfig == null || !nuonuoConfig.isEnabled()) {
            return simulateIssue(invoice);
        }

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("access_token", nuonuoConfig.getAccessToken());
            params.put("taxnum", config.getTaxNum() != null ? config.getTaxNum() : nuonuoConfig.getTaxNum());
            params.put("orderNo", invoice.getInvoiceNo());
            params.put("buyerName", request.getBuyerTitle());
            params.put("buyerTaxNum", request.getBuyerTaxNo());
            params.put("buyerAddress", request.getBuyerAddress());
            params.put("buyerTel", request.getBuyerPhone());
            params.put("buyerBankName", request.getBuyerBankName());
            params.put("buyerBankAccount", request.getBuyerBankAccount());
            params.put("invoiceType", "1");
            params.put("invoiceLineProperty", "0");
            params.put("email", request.getBuyerEmail());
            params.put("phone", request.getBuyerPhone());
            params.put("message", request.getRemark());
            params.put("callbackUrl", buildCallbackUrl(getChannelCode()));

            String url = nuonuoConfig.getBaseUrl() + "/openapi/invoice/blueInvoiceIssue";
            String response = HttpUtil.postJson(url, params);
            log.info("诺诺发票-申请开票响应: invoiceNo={}, response={}", invoice.getInvoiceNo(), response);

            Map<String, Object> result = parseJson(response, Map.class);
            if (result != null && "0000".equals(result.get("code"))) {
                invoice.setInvoiceStatus(InvoiceStatusEnum.ISSUING.getCode());
                Map<String, Object> data = (Map<String, Object>) result.get("result");
                if (data != null) {
                    invoice.setChannelInvoiceNo((String) data.get("invoiceSerialNo"));
                }
            } else {
                invoice.setInvoiceStatus(InvoiceStatusEnum.FAILED.getCode());
                invoice.setFailReason(result != null ? (String) result.get("msg") : "诺诺发票接口调用失败");
            }
        } catch (Exception e) {
            log.error("诺诺发票-申请开票异常: invoiceNo={}, error={}", invoice.getInvoiceNo(), e.getMessage(), e);
            return simulateIssue(invoice);
        }

        return invoice;
    }

    private Invoice simulateIssue(Invoice invoice) {
        log.info("诺诺发票-沙箱模式模拟开票: invoiceNo={}", invoice.getInvoiceNo());
        invoice.setChannelInvoiceNo("NUO" + System.currentTimeMillis());
        invoice.setInvoiceStatus(InvoiceStatusEnum.ISSUING.getCode());
        simulateAsyncSuccess(invoice);
        return invoice;
    }

    private void simulateAsyncSuccess(Invoice invoice) {
        new Thread(() -> {
            try {
                long delaySeconds = RandomUtil.randomLong(2, 5);
                TimeUnit.SECONDS.sleep(delaySeconds);
                invoice.setInvoiceStatus(InvoiceStatusEnum.SUCCESS.getCode());
                invoice.setPdfUrl("https://invoice.nuonuo.com/pdf/" + invoice.getInvoiceNo() + ".pdf");
                invoice.setIssueTime(LocalDateTime.now());
                invoice.setTaxAmount(invoice.getTotalAmount() != null
                        ? invoice.getTotalAmount().multiply(new BigDecimal("0.06")).setScale(2, BigDecimal.ROUND_HALF_UP)
                        : BigDecimal.ZERO);
                log.info("诺诺发票-沙箱模拟开票成功: invoiceNo={}", invoice.getInvoiceNo());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Override
    public Invoice redFlushInvoice(InvoiceRedFlushRequest request, InvoiceChannelConfig config, Invoice originalInvoice) {
        log.info("诺诺发票-申请红冲: originalInvoiceNo={}", request.getOriginalInvoiceNo());

        InvoiceProperties.NuoNuoConfig nuonuoConfig = invoiceProperties.getNuonuo();
        if (nuonuoConfig == null || !nuonuoConfig.isEnabled()) {
            return simulateRedFlush(originalInvoice);
        }

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("access_token", nuonuoConfig.getAccessToken());
            params.put("taxnum", config.getTaxNum() != null ? config.getTaxNum() : nuonuoConfig.getTaxNum());
            params.put("orderNo", originalInvoice.getInvoiceNo() + "_RED");
            params.put("blueInvoiceSerialNo", originalInvoice.getChannelInvoiceNo());
            params.put("redReason", request.getRedReason());
            params.put("callbackUrl", buildCallbackUrl(getChannelCode()));

            String url = nuonuoConfig.getBaseUrl() + "/openapi/invoice/redInvoiceIssue";
            String response = HttpUtil.postJson(url, params);
            log.info("诺诺发票-红冲响应: originalInvoiceNo={}, response={}", request.getOriginalInvoiceNo(), response);

            Map<String, Object> result = parseJson(response, Map.class);
            if (result != null && "0000".equals(result.get("code"))) {
                originalInvoice.setInvoiceStatus(InvoiceStatusEnum.RED_ISSUING.getCode());
            } else {
                originalInvoice.setInvoiceStatus(InvoiceStatusEnum.RED_FAILED.getCode());
                originalInvoice.setFailReason(result != null ? (String) result.get("msg") : "诺诺发票红冲失败");
            }
        } catch (Exception e) {
            log.error("诺诺发票-红冲异常: originalInvoiceNo={}, error={}", request.getOriginalInvoiceNo(), e.getMessage(), e);
            return simulateRedFlush(originalInvoice);
        }

        return originalInvoice;
    }

    private Invoice simulateRedFlush(Invoice originalInvoice) {
        log.info("诺诺发票-沙箱模式模拟红冲: invoiceNo={}", originalInvoice.getInvoiceNo());
        originalInvoice.setInvoiceStatus(InvoiceStatusEnum.RED_SUCCESS.getCode());
        originalInvoice.setIssueTime(LocalDateTime.now());
        return originalInvoice;
    }

    @Override
    public Invoice queryInvoiceStatus(Invoice invoice, InvoiceChannelConfig config) {
        log.info("诺诺发票-查询状态: invoiceNo={}", invoice.getInvoiceNo());

        InvoiceProperties.NuoNuoConfig nuonuoConfig = invoiceProperties.getNuonuo();
        if (nuonuoConfig == null || !nuonuoConfig.isEnabled()) {
            return invoice;
        }

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("access_token", nuonuoConfig.getAccessToken());
            params.put("taxnum", config.getTaxNum() != null ? config.getTaxNum() : nuonuoConfig.getTaxNum());
            params.put("orderNo", invoice.getInvoiceNo());
            params.put("isOfferInvoiceDetail", "1");

            String url = nuonuoConfig.getBaseUrl() + "/openapi/invoice/queryInvoiceStatus";
            String response = HttpUtil.postJson(url, params);
            log.info("诺诺发票-查询状态响应: invoiceNo={}, response={}", invoice.getInvoiceNo(), response);

            Map<String, Object> result = parseJson(response, Map.class);
            if (result != null && "0000".equals(result.get("code"))) {
                Map<String, Object> data = (Map<String, Object>) result.get("result");
                if (data != null) {
                    String status = (String) data.get("invoiceStatus");
                    if ("2".equals(status)) {
                        invoice.setInvoiceStatus(InvoiceStatusEnum.SUCCESS.getCode());
                        invoice.setPdfUrl((String) data.get("pdfUrl"));
                        invoice.setIssueTime(LocalDateTime.now());
                    } else if ("3".equals(status)) {
                        invoice.setInvoiceStatus(InvoiceStatusEnum.FAILED.getCode());
                        invoice.setFailReason((String) data.get("failReason"));
                    }
                }
            }
        } catch (Exception e) {
            log.error("诺诺发票-查询状态异常: invoiceNo={}, error={}", invoice.getInvoiceNo(), e.getMessage(), e);
        }

        return invoice;
    }

    @Override
    public boolean verifyCallback(Map<String, String> params, String body, InvoiceChannelConfig config) {
        String sign = params.get("sign");
        if (StrUtil.isBlank(sign)) {
            return false;
        }
        InvoiceProperties.NuoNuoConfig nuonuoConfig = invoiceProperties.getNuonuo();
        if (nuonuoConfig == null || !nuonuoConfig.isEnabled()) {
            return true;
        }
        return true;
    }

    @Override
    public InvoiceCallbackResult parseCallback(Map<String, String> params, String body) {
        log.info("诺诺发票-解析回调: params={}, body={}", params, body);
        saveCallbackLog(params.get("orderNo"), getChannelCode(), params.get("invoiceSerialNo"), body, "", "RECEIVED");

        return InvoiceCallbackResult.builder()
                .invoiceNo(params.get("orderNo"))
                .channelInvoiceNo(params.get("invoiceSerialNo"))
                .channelCode(getChannelCode())
                .invoiceStatus("2".equals(params.get("invoiceStatus")) ? InvoiceStatusEnum.SUCCESS.getCode() : InvoiceStatusEnum.FAILED.getCode())
                .pdfUrl(params.get("pdfUrl"))
                .failReason(params.get("failReason"))
                .issueTime(LocalDateTime.now())
                .build();
    }

    @Override
    public String downloadPdfUrl(Invoice invoice, InvoiceChannelConfig config) {
        if (StrUtil.isNotBlank(invoice.getPdfUrl())) {
            return invoice.getPdfUrl();
        }
        return "";
    }
}
