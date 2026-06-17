package com.payhub.channel.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UnifiedOrderResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;

    private String msg;

    private String payType;

    private String payParams;

    private String channelTradeNo;

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(code);
    }

    public static UnifiedOrderResponse success(String payType, String payParams, String channelTradeNo) {
        UnifiedOrderResponse response = new UnifiedOrderResponse();
        response.setCode("SUCCESS");
        response.setMsg("下单成功");
        response.setPayType(payType);
        response.setPayParams(payParams);
        response.setChannelTradeNo(channelTradeNo);
        return response;
    }

    public static UnifiedOrderResponse fail(String code, String msg) {
        UnifiedOrderResponse response = new UnifiedOrderResponse();
        response.setCode(code);
        response.setMsg(msg);
        return response;
    }
}
