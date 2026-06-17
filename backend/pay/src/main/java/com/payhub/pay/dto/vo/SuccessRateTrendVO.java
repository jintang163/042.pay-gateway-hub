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
public class SuccessRateTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String date;

    private Long totalCount;

    private Long successCount;

    private BigDecimal successRate;
}
