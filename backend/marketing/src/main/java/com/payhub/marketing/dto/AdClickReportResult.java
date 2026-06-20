package com.payhub.marketing.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class AdClickReportResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String clickNo;
    private String adCode;
    private String targetUrl;
    private BigDecimal cpcPrice;
    private BigDecimal costAmount;
    private Boolean valid;
    private String invalidReason;
}
