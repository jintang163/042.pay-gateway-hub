package com.payhub.merchant.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class SignCodeExampleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String language;

    private String signType;

    private String code;

    private String description;
}
