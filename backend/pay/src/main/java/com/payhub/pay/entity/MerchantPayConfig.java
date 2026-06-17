package com.payhub.pay.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("merchant_pay_config")
public class MerchantPayConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String merchantNo;

    private String payChannel;

    private String payType;

    private String channelCode;

    private BigDecimal feeRate;

    private BigDecimal minFee;

    private BigDecimal maxFee;

    private Integer status;

    private Integer priority;

    private String whitelistIps;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
