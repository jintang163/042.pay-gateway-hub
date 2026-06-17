package com.payhub.settlement.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ReconcileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String reconcileNo;

    private LocalDate reconcileDate;

    private String payChannel;

    private Integer totalCount;

    private Integer matchCount;

    private Integer mismatchCount;

    private Integer reconcileStatus;

    private String reconcileStatusDesc;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
