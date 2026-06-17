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
public class RefundResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String refundNo;

    private Integer refundStatus;
}
