package com.payhub.pay.dto.vo;

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
public class ChannelDistributionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String channelCode;

    private String channelName;

    private Long orderCount;

    private BigDecimal amount;

    private BigDecimal percentage;
}
