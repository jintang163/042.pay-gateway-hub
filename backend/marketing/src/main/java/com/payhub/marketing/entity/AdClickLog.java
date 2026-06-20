package com.payhub.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ad_click_log")
public class AdClickLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String clickNo;

    private String adCode;

    private String merchantNo;

    private String orderNo;

    private BigDecimal payAmount;

    private String position;

    private BigDecimal cpcPrice;

    private BigDecimal costAmount;

    private String userAgent;

    private String clientIp;

    private String deviceId;

    private String refererUrl;

    private String targetUrl;

    private LocalDateTime clickTime;

    private LocalDate clickDate;

    private Integer status;

    private String invalidReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
