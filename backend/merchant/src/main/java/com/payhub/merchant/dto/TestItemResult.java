package com.payhub.merchant.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class TestItemResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String itemCode;

    private String itemName;

    private String itemCategory;

    private String status;

    private String statusDesc;

    private Long durationMs;

    private String expectedValue;

    private String actualValue;

    private String message;

    private String detail;

    private String suggestion;
}
