package com.payhub.common.enums;

import lombok.Getter;

@Getter
public enum SandboxSceneEnum {

    SUCCESS("success", "支付成功", "模拟正常的支付成功流程，包含回调通知"),
    FAILED("failed", "支付失败", "模拟支付失败场景，验证异常处理逻辑"),
    TIMEOUT("timeout", "支付超时", "模拟支付超时，验证订单状态处理"),
    INSUFFICIENT_BALANCE("insufficient_balance", "余额不足", "模拟账户余额不足场景"),
    REPEAT_NOTIFY("repeat_notify", "重复通知", "模拟重复异步通知，验证幂等性"),
    SIGN_ERROR("sign_error", "签名错误", "模拟签名校验失败场景"),
    AMOUNT_MISMATCH("amount_mismatch", "金额不匹配", "模拟回调金额与订单金额不一致"),
    REFUND_SUCCESS("refund_success", "退款成功", "模拟正常退款成功流程"),
    REFUND_FAILED("refund_failed", "退款失败", "模拟退款失败场景"),
    CHANNEL_ERROR("channel_error", "通道异常", "模拟支付通道返回系统异常");

    private final String code;
    private final String name;
    private final String description;

    SandboxSceneEnum(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static SandboxSceneEnum getByCode(String code) {
        if (code == null) {
            return SUCCESS;
        }
        for (SandboxSceneEnum scene : values()) {
            if (scene.getCode().equalsIgnoreCase(code)) {
                return scene;
            }
        }
        return SUCCESS;
    }
}
