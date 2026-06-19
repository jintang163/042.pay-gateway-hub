package com.payhub.pay.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pay_order")
public class PayOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private String merchantNo;

    private String merchantOrderNo;

    private String linkCode;

    private String couponCode;

    private String activityCode;

    private BigDecimal payAmount;

    private BigDecimal couponDiscount;

    private BigDecimal activityDiscount;

    private BigDecimal actualAmount;

    private BigDecimal feeAmount;

    private String payChannel;

    private String payType;

    private String userIdentity;

    private String productSubject;

    private String productDetail;

    private String notifyUrl;

    private String clientIp;

    private String extraParams;

    private Integer payStatus;

    private String channelTradeNo;

    private LocalDateTime payTime;

    private LocalDateTime expireTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
