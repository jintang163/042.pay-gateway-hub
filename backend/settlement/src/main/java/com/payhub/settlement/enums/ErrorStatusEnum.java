package com.payhub.settlement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorStatusEnum {

    PENDING(0, "待处理"),
    PROCESSING(1, "处理中"),
    SUCCESS(2, "处理成功"),
    FAIL(3, "处理失败"),
    CLOSED(4, "已关闭");

    private final Integer code;
    private final String desc;

    public static ErrorStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ErrorStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
