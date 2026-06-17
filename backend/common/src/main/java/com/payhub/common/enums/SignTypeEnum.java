package com.payhub.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SignTypeEnum {

    MD5("MD5", "MD5签名"),
    RSA("RSA", "RSA签名"),
    SM2("SM2", "国密SM2签名");

    private final String code;

    private final String desc;

    public static SignTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (SignTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
