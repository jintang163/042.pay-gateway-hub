package com.payhub.merchant.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class MerchantConfigTestReport implements Serializable {

    private static final long serialVersionUID = 1L;

    private String merchantNo;

    private String merchantName;

    private Integer totalTests;

    private Integer passedTests;

    private Integer failedTests;

    private String overallStatus;

    private String overallStatusDesc;

    private Long totalTimeMs;

    private String testTime;

    private List<TestItemResult> items;

    private String summary;
}
