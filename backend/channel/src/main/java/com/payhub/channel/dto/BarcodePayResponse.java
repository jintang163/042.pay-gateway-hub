package com.payhub.channel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarcodePayResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private String channelTradeNo;

    private String payStatus;

    private BigDecimal payAmount;

    private LocalDateTime payTime;

    private String buyerUserId;

    private String buyerLogonId;

    private String code;

    private String msg;

    private String subCode;

    private String subMsg;

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(payStatus) || "10000".equals(code);
    }

    public boolean isPaying() {
        return "PAYING".equalsIgnoreCase(payStatus) || "USERPAYING".equalsIgnoreCase(subCode);
    }

    public static BarcodePayResponse success(String orderNo, String channelTradeNo, BigDecimal amount,
                                              LocalDateTime payTime, String buyerUserId, String buyerLogonId) {
        return BarcodePayResponse.builder()
                .orderNo(orderNo)
                .channelTradeNo(channelTradeNo)
                .payStatus("SUCCESS")
                .payAmount(amount)
                .payTime(payTime)
                .buyerUserId(buyerUserId)
                .buyerLogonId(buyerLogonId)
                .code("10000")
                .msg("Success")
                .build();
    }

    public static BarcodePayResponse fail(String code, String msg) {
        return BarcodePayResponse.builder()
                .payStatus("FAIL")
                .code(code)
                .msg(msg)
                .build();
    }

    public static BarcodePayResponse paying(String orderNo, String subMsg) {
        return BarcodePayResponse.builder()
                .orderNo(orderNo)
                .payStatus("PAYING")
                .subCode("USERPAYING")
                .subMsg(subMsg != null ? subMsg : "用户支付中，需要输入密码")
                .build();
    }
}
