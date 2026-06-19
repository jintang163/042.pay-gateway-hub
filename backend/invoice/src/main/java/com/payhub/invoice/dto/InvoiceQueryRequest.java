package com.payhub.invoice.dto;

import com.payhub.common.dto.BasePageQuery;
import lombok.Data;

@Data
public class InvoiceQueryRequest extends BasePageQuery {

    private String invoiceNo;

    private String merchantNo;

    private String orderNo;

    private String channelInvoiceNo;

    private String channelCode;

    private Integer invoiceType;

    private Integer invoiceStatus;

    private String buyerTitle;

    private String startTime;

    private String endTime;
}
