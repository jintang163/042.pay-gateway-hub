package com.payhub.marketing.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AdDisplayVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String adCode;
    private String adTitle;
    private String adDescription;
    private String adImageUrl;
    private String targetUrl;
    private String position;
    private BigDecimal cpcPrice;
}
