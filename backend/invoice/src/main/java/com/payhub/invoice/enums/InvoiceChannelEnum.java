package com.payhub.invoice.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InvoiceChannelEnum {

    NUONUO("NUONUO", "诺诺发票"),
    BAIWANG("BAIWANG", "百望发票");

    private final String code;
    private final String desc;

    public static InvoiceChannelEnum getByCode(String code) {
        for (InvoiceChannelEnum e : values()) {
            if (e.getCode().equalsIgnoreCase(code)) {
                return e;
            }
        }
        return null;
    }
}
