package com.payhub.invoice.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InvoiceStatusEnum {

    PENDING(0, "待开票"),
    ISSUING(1, "开票中"),
    SUCCESS(2, "开票成功"),
    FAILED(3, "开票失败"),
    RED_PENDING(10, "待红冲"),
    RED_ISSUING(11, "红冲中"),
    RED_SUCCESS(12, "红冲成功"),
    RED_FAILED(13, "红冲失败");

    private final Integer code;
    private final String desc;

    public static InvoiceStatusEnum getByCode(Integer code) {
        if (code == null) return null;
        for (InvoiceStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
