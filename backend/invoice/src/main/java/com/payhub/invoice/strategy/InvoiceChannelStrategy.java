package com.payhub.invoice.strategy;

import com.payhub.invoice.dto.InvoiceApplyRequest;
import com.payhub.invoice.dto.InvoiceCallbackResult;
import com.payhub.invoice.dto.InvoiceRedFlushRequest;
import com.payhub.invoice.entity.Invoice;
import com.payhub.invoice.entity.InvoiceChannelConfig;

import java.util.Map;

public interface InvoiceChannelStrategy {

    String getChannelCode();

    Invoice issueInvoice(InvoiceApplyRequest request, InvoiceChannelConfig config, Invoice invoice);

    Invoice redFlushInvoice(InvoiceRedFlushRequest request, InvoiceChannelConfig config, Invoice originalInvoice);

    Invoice queryInvoiceStatus(Invoice invoice, InvoiceChannelConfig config);

    boolean verifyCallback(Map<String, String> params, String body, InvoiceChannelConfig config);

    InvoiceCallbackResult parseCallback(Map<String, String> params, String body);

    String downloadPdfUrl(Invoice invoice, InvoiceChannelConfig config);
}
