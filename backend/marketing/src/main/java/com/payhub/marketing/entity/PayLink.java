package com.payhub.marketing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pay_link")
public class PayLink implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String linkCode;

    private String merchantNo;

    private String title;

    private BigDecimal fixedAmount;

    private Boolean amountEditable;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private String payChannel;

    private String productSubject;

    private String productDetail;

    private String notifyUrl;

    private String redirectUrl;

    private LocalDateTime expireTime;

    private Boolean singleUse;

    private Integer maxUseCount;

    private Integer usedCount;

    private Integer status;

    private String remark;

    private String operatorId;

    private String operatorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
