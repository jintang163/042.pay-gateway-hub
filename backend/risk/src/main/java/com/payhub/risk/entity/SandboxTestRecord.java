package com.payhub.risk.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sandbox_test_record")
public class SandboxTestRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String testId;

    private String merchantNo;

    private String testScene;

    private String testName;

    private String payChannel;

    private String payType;

    private BigDecimal payAmount;

    private String testParams;

    private Integer expectResult;

    private Integer actualResult;

    private String responseData;

    private String notifyResult;

    private String errorMsg;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long costTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
