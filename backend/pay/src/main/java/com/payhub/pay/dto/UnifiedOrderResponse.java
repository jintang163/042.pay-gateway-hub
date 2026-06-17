package com.payhub.pay.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedOrderResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private String payType;

    private String payParams;

    private Integer payStatus;
}
