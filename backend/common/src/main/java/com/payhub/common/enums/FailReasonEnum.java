package com.payhub.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FailReasonEnum {

    INSUFFICIENT_BALANCE("FAIL_INSUFFICIENT_BALANCE", "用户余额不足", "BALANCE",
            "请提醒用户检查支付账户余额或更换支付方式",
            "检测到渠道响应返回资金不足类错误码 (如 ACQ.NOT_ENOUGH_MONEY / NOTENOUGH)", 1),
    RISK_REJECT("FAIL_RISK_REJECT", "风控系统拦截", "RISK",
            "请降低交易金额或让用户完成身份验证后重新下单",
            "根据 risk_control_log 记录, 命中高风险规则或黑名单, 处理结果=拒绝", 2),
    CHANNEL_TIMEOUT("FAIL_CHANNEL_TIMEOUT", "支付通道超时", "CHANNEL",
            "建议等待1分钟后重新查询订单状态, 或更换支付通道",
            "通道请求 cost_time 超过阈值或 error_msg 含 timeout / connect timeout 关键字", 3),
    CHANNEL_SYSTEM_ERROR("FAIL_CHANNEL_SYSTEM_ERROR", "通道系统异常", "CHANNEL",
            "通道方服务暂时不可用, 请稍后重试或切换备用通道",
            "通道响应码为 SYSTEM_ERROR / SERVER_ERROR / 5xx 或通道返回空", 4),
    SIGN_VERIFY_FAILED("FAIL_SIGN_VERIFY_FAILED", "签名验证失败", "SIGN",
            "请检查商户签名密钥配置是否正确, 并使用规范的签名算法",
            "SignInterceptor 校验失败或通道响应签名校验失败", 5),
    PARAM_INVALID("FAIL_PARAM_INVALID", "请求参数非法", "PARAM",
            "请参照API文档检查必填字段、格式与金额范围 (金额需>=0.01)",
            "下单前参数校验不通过, 或通道返回参数类错误码", 6),
    MERCHANT_LIMIT("FAIL_MERCHANT_LIMIT", "商户额度/权限受限", "MERCHANT",
            "请联系运营提升商户单日限额或开通对应支付通道权限",
            "商户状态异常 / 超出日累计限额 / 对应通道未在 merchant_pay_config 中启用", 7),
    USER_CANCEL("FAIL_USER_CANCEL", "用户主动取消", "USER",
            "用户在支付页面主动取消, 无需处理, 可引导重新发起",
            "订单状态被标记为关闭且来源是支付页面取消回调", 8),
    EXPIRED("FAIL_EXPIRED", "订单超时关闭", "SYSTEM",
            "请重新发起支付, 建议将 expireTime 设置得更宽裕",
            "订单在 expireTime 之前未完成支付, 被定时任务或用户再次下单关闭", 9),
    UNKNOWN("FAIL_UNKNOWN", "未知原因", "OTHER",
            "请提交工单联系技术支持并附上订单号以便进一步排查",
            "未匹配到以上规则, 需要人工核查通道响应与日志", 99);

    private final String code;
    private final String message;
    private final String category;
    private final String suggestion;
    private final String ruleDescription;
    private final int priority;

    public static FailReasonEnum getByCode(String code) {
        if (code == null) return UNKNOWN;
        for (FailReasonEnum reason : values()) {
            if (reason.getCode().equals(code)) return reason;
        }
        return UNKNOWN;
    }
}
