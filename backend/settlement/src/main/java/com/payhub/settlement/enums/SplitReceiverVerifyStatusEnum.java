package com.payhub.settlement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SplitReceiverVerifyStatusEnum {

    UNVERIFIED(0, "未认证"),
    VERIFYING(1, "认证中"),
    VERIFIED(2, "已认证"),
    FAILED(3, "认证失败");

    private final Integer code;
    private final String desc;

    public static SplitReceiverVerifyStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (SplitReceiverVerifyStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
