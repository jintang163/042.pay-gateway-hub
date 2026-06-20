package com.payhub.settlement.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MerchantWithdrawStatusEnum {

    PENDING_AUDIT(0, "待审核"),
    AUDIT_PASSED(1, "审核通过"),
    AUDIT_REJECTED(2, "审核拒绝"),
    TRANSFERRING(3, "转账中"),
    SUCCESS(4, "提现成功"),
    FAILED(5, "提现失败"),
    ARRIVED(6, "已到账");

    private final Integer code;
    private final String desc;

    public static MerchantWithdrawStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (MerchantWithdrawStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
