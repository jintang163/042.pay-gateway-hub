package com.payhub.invoice.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InvoiceTypeEnum {

    BLUE(1, "蓝票"),
    RED(2, "红票");

    private final Integer code;
    private final String desc;

    public static InvoiceTypeEnum getByCode(Integer code) {
        if (code == null) return null;
        for (InvoiceTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
