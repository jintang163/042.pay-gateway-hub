package com.payhub.risk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxTestResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String testId;

    private String merchantNo;

    private String testScene;

    private String testName;

    private String payChannel;

    private String payType;

    private BigDecimal payAmount;

    private Integer expectResult;

    private Integer actualResult;

    private Boolean success;

    private String responseData;

    private String notifyResult;

    private String errorMsg;

    private Long costTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
