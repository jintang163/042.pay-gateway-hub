package com.payhub.invoice.dto;

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
public class InvoiceItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String itemName;

    private String itemCode;

    private String specification;

    private String unit;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private BigDecimal taxAmount;

    private String taxRate;

    private Integer taxIncludedFlag;
}
