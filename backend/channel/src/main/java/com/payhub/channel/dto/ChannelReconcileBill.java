package com.payhub.channel.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChannelReconcileBill implements Serializable {

    private static final long serialVersionUID = 1L;

    private String payChannel;

    private String billDate;

    private String merchantNo;

    private Integer totalCount;

    private BigDecimal totalAmount;

    private List<ChannelReconcileItem> items;

    @Data
    public static class ChannelReconcileItem implements Serializable {

        private static final long serialVersionUID = 1L;

        private String channelTradeNo;

        private String merchantOrderNo;

        private String merchantNo;

        private BigDecimal tradeAmount;

        private String tradeStatus;

        private LocalDateTime tradeTime;

        private BigDecimal feeAmount;

        private String buyerAccount;

        private String extraInfo;
    }
}
