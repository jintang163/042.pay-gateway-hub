package com.payhub.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PayStatusEnum {

    PENDING(0, "待支付"),
    SUCCESS(1, "支付成功"),
    FAIL(2, "支付失败"),
    CLOSED(3, "已关闭"),
    REFUNDING(4, "退款中"),
    REFUNDED(5, "已退款");

    private final Integer code;

    private final String desc;

    public static PayStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (PayStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
