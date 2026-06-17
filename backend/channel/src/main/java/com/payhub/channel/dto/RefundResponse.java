package com.payhub.channel.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RefundResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;

    private String msg;

    private String channelRefundNo;

    private String refundStatus;

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(code);
    }

    public static RefundResponse success(String channelRefundNo, String refundStatus) {
        RefundResponse response = new RefundResponse();
        response.setCode("SUCCESS");
        response.setMsg("退款申请提交成功");
        response.setChannelRefundNo(channelRefundNo);
        response.setRefundStatus(refundStatus);
        return response;
    }

    public static RefundResponse fail(String code, String msg) {
        RefundResponse response = new RefundResponse();
        response.setCode(code);
        response.setMsg(msg);
        return response;
    }
}
