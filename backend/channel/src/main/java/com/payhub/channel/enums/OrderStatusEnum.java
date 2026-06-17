package com.payhub.channel.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatusEnum {

    PENDING("PENDING", "待支付"),
    SUCCESS("SUCCESS", "支付成功"),
    FAILED("FAILED", "支付失败"),
    CLOSED("CLOSED", "订单关闭"),
    REFUNDING("REFUNDING", "退款中"),
    REFUNDED("REFUNDED", "已退款");

    private final String code;
    private final String desc;
}
