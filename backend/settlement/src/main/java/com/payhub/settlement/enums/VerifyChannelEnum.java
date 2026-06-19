package com.payhub.settlement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VerifyChannelEnum {

    BANK_CARD_FOUR(1, "银行卡四要素"),
    BANK_CARD_THREE(2, "银行卡三要素"),
    FACE_RECOGNITION(3, "人脸识别");

    private final Integer code;
    private final String desc;

    public static VerifyChannelEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (VerifyChannelEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
