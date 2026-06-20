package com.payhub.marketing.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AdClickReportRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "广告编号不能为空")
    private String adCode;

    private String orderNo;

    private BigDecimal payAmount;

    private String position;

    private String deviceId;

    private String refererUrl;
}
