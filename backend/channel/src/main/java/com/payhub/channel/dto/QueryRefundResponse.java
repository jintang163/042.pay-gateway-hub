package com.payhub.channel.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QueryRefundResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;

    private String msg;

    private String refundStatus;

    private BigDecimal refundAmount;

    private String channelRefundNo;

    private LocalDateTime refundTime;

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(code);
    }

    public static QueryRefundResponse success(String refundStatus, BigDecimal refundAmount, String channelRefundNo, LocalDateTime refundTime) {
        QueryRefundResponse response = new QueryRefundResponse();
        response.setCode("SUCCESS");
        response.setMsg("查询成功");
        response.setRefundStatus(refundStatus);
        response.setRefundAmount(refundAmount);
        response.setChannelRefundNo(channelRefundNo);
        response.setRefundTime(refundTime);
        return response;
    }

    public static QueryRefundResponse fail(String code, String msg) {
        QueryRefundResponse response = new QueryRefundResponse();
        response.setCode(code);
        response.setMsg(msg);
        return response;
    }
}
