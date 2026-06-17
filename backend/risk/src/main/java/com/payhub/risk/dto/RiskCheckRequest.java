package com.payhub.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskCheckRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String merchantNo;

    private String orderNo;

    private String clientIp;

    private String userIdentity;

    private BigDecimal payAmount;

    private String payChannel;

    private String payType;

    private String requestParams;
}
