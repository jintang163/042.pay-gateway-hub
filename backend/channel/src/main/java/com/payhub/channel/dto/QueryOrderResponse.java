package com.payhub.channel.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QueryOrderResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;

    private String msg;

    private String orderStatus;

    private BigDecimal amount;

    private String channelTradeNo;

    private LocalDateTime payTime;

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(code);
    }

    public static QueryOrderResponse success(String orderStatus, BigDecimal amount, String channelTradeNo, LocalDateTime payTime) {
        QueryOrderResponse response = new QueryOrderResponse();
        response.setCode("SUCCESS");
        response.setMsg("查询成功");
        response.setOrderStatus(orderStatus);
        response.setAmount(amount);
        response.setChannelTradeNo(channelTradeNo);
        response.setPayTime(payTime);
        return response;
    }

    public static QueryOrderResponse fail(String code, String msg) {
        QueryOrderResponse response = new QueryOrderResponse();
        response.setCode(code);
        response.setMsg(msg);
        return response;
    }
}
