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
public class FacePayResponse implements Serializable {

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

    private String faceAuthToken;

    private String openId;

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(payStatus) || "10000".equals(code);
    }

    public boolean isPaying() {
        return "PAYING".equalsIgnoreCase(payStatus) || "USERPAYING".equalsIgnoreCase(subCode);
    }

    public static FacePayResponse success(String orderNo, String channelTradeNo, BigDecimal amount,
                                           LocalDateTime payTime, String buyerUserId, String buyerLogonId, String openId) {
        return FacePayResponse.builder()
                .orderNo(orderNo)
                .channelTradeNo(channelTradeNo)
                .payStatus("SUCCESS")
                .payAmount(amount)
                .payTime(payTime)
                .buyerUserId(buyerUserId)
                .buyerLogonId(buyerLogonId)
                .openId(openId)
                .code("10000")
                .msg("Success")
                .build();
    }

    public static FacePayResponse fail(String code, String msg) {
        return FacePayResponse.builder()
                .payStatus("FAIL")
                .code(code)
                .msg(msg)
                .build();
    }

    public static FacePayResponse paying(String orderNo, String subMsg) {
        return FacePayResponse.builder()
                .orderNo(orderNo)
                .payStatus("PAYING")
                .subCode("USERPAYING")
                .subMsg(subMsg != null ? subMsg : "用户刷脸支付中，请等待")
                .build();
    }

    public static FacePayResponse needAuth(String faceAuthToken, String subMsg) {
        return FacePayResponse.builder()
                .payStatus("NEED_AUTH")
                .subCode("FACE_AUTH_REQUIRED")
                .subMsg(subMsg != null ? subMsg : "需要刷脸验证")
                .faceAuthToken(faceAuthToken)
                .build();
    }
}
