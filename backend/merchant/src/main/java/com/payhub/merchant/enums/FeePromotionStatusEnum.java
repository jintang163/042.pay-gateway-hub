package com.payhub.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FeePromotionStatusEnum {

    DRAFT(0, "草稿"),
    NOT_STARTED(1, "未开始"),
    ONGOING(2, "进行中"),
    ENDED(3, "已结束"),
    DISABLED(4, "已停用");

    private final Integer code;
    private final String desc;

    public static FeePromotionStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FeePromotionStatusEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
