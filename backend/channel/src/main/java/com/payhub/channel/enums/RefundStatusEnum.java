package com.payhub.channel.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RefundStatusEnum {

    PENDING("PENDING", "退款处理中"),
    SUCCESS("SUCCESS", "退款成功"),
    FAILED("FAILED", "退款失败"),
    PROCESSING("PROCESSING", "退款中");

    private final String code;
    private final String desc;
}
