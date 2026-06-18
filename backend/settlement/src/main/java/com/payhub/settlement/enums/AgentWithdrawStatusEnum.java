package com.payhub.settlement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AgentWithdrawStatusEnum {

    PENDING(0, "待审核"),
    APPROVED(1, "审核通过"),
    REJECTED(2, "审核拒绝"),
    TRANSFERRING(3, "转账中"),
    SUCCESS(4, "提现成功"),
    FAILED(5, "提现失败");

    private final Integer code;
    private final String desc;

    public static AgentWithdrawStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AgentWithdrawStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
