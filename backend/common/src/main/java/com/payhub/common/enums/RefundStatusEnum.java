package com.payhub.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RefundStatusEnum {

    PROCESSING(0, "处理中"),
    SUCCESS(1, "退款成功"),
    FAIL(2, "退款失败");

    private final Integer code;

    private final String desc;

    public static RefundStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (RefundStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
