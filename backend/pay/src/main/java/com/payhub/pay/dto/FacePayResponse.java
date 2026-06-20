package com.payhub.pay.dto;

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

    private String merchantOrderNo;

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

    private String retryUrl;

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(payStatus);
    }

    public boolean isPaying() {
        return "PAYING".equalsIgnoreCase(payStatus) || "USERPAYING".equalsIgnoreCase(subCode);
    }

    public boolean isNeedAuth() {
        return "NEED_AUTH".equalsIgnoreCase(payStatus) || "FACE_AUTH_REQUIRED".equalsIgnoreCase(subCode);
    }

    public boolean isNeedRetry() {
        return isPaying() || isNeedAuth();
    }
}
