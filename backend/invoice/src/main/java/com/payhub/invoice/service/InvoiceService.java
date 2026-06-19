package com.payhub.invoice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.payhub.invoice.dto.*;
import com.payhub.invoice.entity.Invoice;

import java.util.List;

public interface InvoiceService extends IService<Invoice> {

    InvoiceResult applyInvoice(InvoiceApplyRequest request);

    InvoiceResult redFlushInvoice(InvoiceRedFlushRequest request);

    List<InvoiceResult> batchApplyInvoice(List<InvoiceApplyRequest> requests);

    InvoiceResult getInvoiceDetail(String invoiceNo, String merchantNo);

    IPage<InvoiceResult> listPage(InvoiceQueryRequest request);

    String handleNotify(String channel, java.util.Map<String, String> params, String body);

    String downloadPdf(String invoiceNo, String merchantNo);

    InvoiceResult queryInvoiceStatus(String invoiceNo, String merchantNo);
}
