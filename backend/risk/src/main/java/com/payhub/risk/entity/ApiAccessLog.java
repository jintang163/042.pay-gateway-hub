package com.payhub.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("api_access_log")
public class ApiAccessLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String merchantNo;

    private String apiPath;

    private String apiMethod;

    private String clientIp;

    private String userAgent;

    private String requestId;

    private Integer httpStatus;

    private Long responseTime;

    private String errorCode;

    private String errorMsg;

    private LocalDateTime accessTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
