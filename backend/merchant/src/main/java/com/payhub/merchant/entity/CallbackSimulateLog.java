package com.payhub.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("callback_simulate_log")
public class CallbackSimulateLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("log_no")
    private String logNo;

    @TableField("merchant_no")
    private String merchantNo;

    @TableField("merchant_name")
    private String merchantName;

    @TableField("order_no")
    private String orderNo;

    @TableField("callback_url")
    private String callbackUrl;

    @TableField(value = "callback_type", defaultValue = "PAY")
    private String callbackType;

    @TableField("simulate_status")
    private String simulateStatus;

    @TableField(value = "sign_type", defaultValue = "MD5")
    private String signType;

    @TableField("request_headers")
    private String requestHeaders;

    @TableField("request_body")
    private String requestBody;

    @TableField("response_http_status")
    private Integer responseHttpStatus;

    @TableField("response_body")
    private String responseBody;

    @TableField("response_time_ms")
    private Integer responseTimeMs;

    @TableField(value = "callback_status", defaultValue = "0")
    private Integer callbackStatus;

    @TableField(value = "retry_count", defaultValue = "0")
    private Integer retryCount;

    @TableField("operator_id")
    private String operatorId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("remark")
    private String remark;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
