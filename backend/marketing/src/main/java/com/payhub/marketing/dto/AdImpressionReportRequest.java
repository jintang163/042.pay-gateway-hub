package com.payhub.marketing.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class AdImpressionReportRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> codes;

    private String merchantNo;

    private String orderNo;

    private BigDecimal payAmount;

    private String position;

    private String deviceId;

    private String refererUrl;

    private String clientIp;
}
