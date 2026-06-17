package com.payhub.settlement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("settlement_record")
public class SettlementRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String settlementNo;

    private String merchantNo;

    private LocalDate settleDate;

    private BigDecimal totalAmount;

    private BigDecimal feeAmount;

    private BigDecimal actualSettleAmount;

    private Integer orderCount;

    private Integer settleStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
