package com.payhub.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("agent_profit_record")
public class AgentProfitRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String profitNo;

    private String orderNo;

    private String merchantNo;

    private String merchantName;

    private String agentMerchantNo;

    private String agentMerchantName;

    private Integer agentLevel;

    private BigDecimal orderAmount;

    private BigDecimal feeAmount;

    private BigDecimal profitAmount;

    private BigDecimal commissionRate;

    private String settleDate;

    private Integer profitStatus;

    private String settlementId;

    private String settlementNo;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
