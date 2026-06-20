package com.payhub.pay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregateCodeOrderResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private String merchantOrderNo;

    private BigDecimal payAmount;

    private String qrCodeUrl;

    private String qrCodeBase64;

    private Integer qrCodeSize;

    private Integer payStatus;

    private LocalDateTime expireTime;

    private String payChannel;

    private String payParams;

    private String codeUrl;

    private String channelOrderNo;

    private String channelCode;
}
