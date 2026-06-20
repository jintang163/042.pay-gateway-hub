package com.payhub.settlement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VerifyChannelEnum {

    BANK_CARD_FOUR(1, "银行卡四要素"),
    BANK_CARD_THREE(2, "银行卡三要素"),
    FACE_RECOGNITION(3, "人脸识别"),
    ID_CARD_SECOND_GEN(4, "二代身份证核验"),
    ID_CARD_THIRD_GEN(5, "三代身份证核验"),
    ID_CARD_LIVENESS(6, "身份证+活体检测");

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
