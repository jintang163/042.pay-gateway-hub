package com.payhub.merchant.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ApiKeyVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String merchantNo;

    private String md5Key;

    private String rsaPublicKey;

    private String rsaPrivateKey;

    private String sm2PublicKey;

    private String sm2PrivateKey;
}
