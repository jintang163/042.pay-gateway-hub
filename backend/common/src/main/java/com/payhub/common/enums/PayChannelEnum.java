package com.payhub.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PayChannelEnum {

    ALIPAY("ALIPAY", "支付宝"),
    WECHAT_PAY("WECHAT_PAY", "微信支付"),
    UNION_PAY("UNION_PAY", "银联支付");

    private final String code;

    private final String desc;

    public static PayChannelEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (PayChannelEnum channel : values()) {
            if (channel.getCode().equals(code)) {
                return channel;
            }
        }
        return null;
    }
}
