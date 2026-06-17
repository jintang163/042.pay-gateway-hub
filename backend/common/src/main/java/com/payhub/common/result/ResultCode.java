package com.payhub.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),

    MERCHANT_NOT_EXIST(1001, "商户不存在"),
    MERCHANT_AUDIT_PENDING(1002, "商户审核中"),
    MERCHANT_AUDIT_REJECTED(1003, "商户审核未通过"),
    MERCHANT_DISABLED(1004, "商户已禁用"),
    MERCHANT_APPLY_EXIST(1005, "该营业执照已提交入驻申请"),
    MERCHANT_USERNAME_EXIST(1006, "用户名已存在"),
    MERCHANT_LOGIN_ERROR(1007, "用户名或密码错误"),

    SMS_CODE_ERROR(2001, "短信验证码错误"),
    SIGN_VERIFY_ERROR(2002, "签名验证失败"),

    CALLBACK_TEST_FAIL(3001, "回调测试失败"),

    ORDER_NOT_EXIST(4001, "订单不存在"),
    ORDER_ALREADY_PAID(4002, "订单已支付"),
    ORDER_CLOSED(4003, "订单已关闭"),
    ORDER_PAY_FAIL(4004, "订单支付失败"),

    REFUND_NOT_EXIST(4101, "退款订单不存在"),
    REFUND_AMOUNT_EXCEED(4102, "退款金额超过可退金额"),
    REFUND_PROCESSING(4103, "退款处理中"),
    REFUND_FAIL(4104, "退款失败"),

    CHANNEL_NOT_SUPPORT(4201, "不支持的支付通道"),
    CHANNEL_CONFIG_ERROR(4202, "支付通道配置错误"),
    CHANNEL_CALL_ERROR(4203, "支付通道调用失败"),

    RISK_BLOCKED(4301, "请求被风控拦截"),
    RISK_AMOUNT_LIMIT(4302, "金额超出限制"),
    RISK_FREQUENCY_LIMIT(4303, "请求频率超限"),
    IP_NOT_IN_WHITELIST(4304, "IP不在白名单"),

    SETTLEMENT_NOT_EXIST(4401, "结算记录不存在");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
