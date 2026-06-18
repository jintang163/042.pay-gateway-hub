package com.payhub.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("merchant_config_test_log")
public class MerchantConfigTestLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("log_no")
    private String logNo;

    @TableField("merchant_no")
    private String merchantNo;

    @TableField("merchant_name")
    private String merchantName;

    @TableField("total_tests")
    private Integer totalTests;

    @TableField("passed_tests")
    private Integer passedTests;

    @TableField("failed_tests")
    private Integer failedTests;

    @TableField("overall_status")
    private String overallStatus;

    @TableField("overall_status_desc")
    private String overallStatusDesc;

    @TableField("callback_url")
    private String callbackUrl;

    @TableField("sign_type")
    private String signType;

    @TableField("total_time_ms")
    private Integer totalTimeMs;

    @TableField("items_json")
    private String itemsJson;

    @TableField("summary")
    private String summary;

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

    @TableLogic
    private Integer deleted;
}
