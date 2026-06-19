package com.payhub.invoice.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payhub.common.context.CurrentUserContext;
import com.payhub.common.result.Result;
import com.payhub.invoice.dto.*;
import com.payhub.invoice.service.InvoiceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.BufferedReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/invoice")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @PostMapping("/apply")
    public Result<InvoiceResult> applyInvoice(@Valid @RequestBody InvoiceApplyRequest request) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        request.setMerchantNo(merchantNo);
        InvoiceResult result = invoiceService.applyInvoice(request);
        return Result.success(result);
    }

    @PostMapping("/red-flush")
    public Result<InvoiceResult> redFlushInvoice(@Valid @RequestBody InvoiceRedFlushRequest request) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        request.setMerchantNo(merchantNo);
        InvoiceResult result = invoiceService.redFlushInvoice(request);
        return Result.success(result);
    }

    @PostMapping("/batch-apply")
    public Result<List<InvoiceResult>> batchApplyInvoice(@Valid @RequestBody List<InvoiceApplyRequest> requests) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        for (InvoiceApplyRequest request : requests) {
            request.setMerchantNo(merchantNo);
        }
        List<InvoiceResult> results = invoiceService.batchApplyInvoice(requests);
        return Result.success(results);
    }

    @GetMapping("/list")
    public Result<IPage<InvoiceResult>> list(InvoiceQueryRequest request) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        request.setMerchantNo(merchantNo);
        IPage<InvoiceResult> page = invoiceService.listPage(request);
        return Result.success(page);
    }

    @GetMapping("/{invoiceNo}")
    public Result<InvoiceResult> detail(@PathVariable String invoiceNo) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        InvoiceResult result = invoiceService.getInvoiceDetail(invoiceNo, merchantNo);
        return Result.success(result);
    }

    @GetMapping("/{invoiceNo}/status")
    public Result<InvoiceResult> queryStatus(@PathVariable String invoiceNo) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        InvoiceResult result = invoiceService.queryInvoiceStatus(invoiceNo, merchantNo);
        return Result.success(result);
    }

    @GetMapping("/{invoiceNo}/download")
    public void downloadPdf(@PathVariable String invoiceNo,
                            HttpServletResponse response) throws Exception {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        String pdfUrl = invoiceService.downloadPdf(invoiceNo, merchantNo);

        URL url = new URL(pdfUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + invoiceNo + ".pdf\"");

        try (InputStream is = conn.getInputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                response.getOutputStream().write(buffer, 0, len);
            }
            response.getOutputStream().flush();
        }
    }

    @GetMapping("/{invoiceNo}/pdf-url")
    public Result<String> getPdfUrl(@PathVariable String invoiceNo) {
        String merchantNo = CurrentUserContext.getCurrentMerchantNo();
        String pdfUrl = invoiceService.downloadPdf(invoiceNo, merchantNo);
        return Result.success(pdfUrl);
    }

    @PostMapping("/notify/{channel}")
    public String notify(@PathVariable String channel, HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            params.put(name, request.getParameter(name));
        }
        StringBuilder body = new StringBuilder();
        try {
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        } catch (Exception e) {
            log.warn("读取发票回调请求体失败", e);
        }
        return invoiceService.handleNotify(channel, params, body.toString());
    }
}
