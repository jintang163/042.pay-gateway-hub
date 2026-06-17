package com.payhub.channel.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PayTypeEnum {

    H5("H5", "H5支付"),
    NATIVE("NATIVE", "扫码支付"),
    JSAPI("JSAPI", "公众号/小程序支付"),
    APP("APP", "APP支付");

    private final String code;
    private final String desc;

    public static PayTypeEnum getByCode(String code) {
        for (PayTypeEnum payType : values()) {
            if (payType.getCode().equals(code)) {
                return payType;
            }
        }
        return null;
    }
}
