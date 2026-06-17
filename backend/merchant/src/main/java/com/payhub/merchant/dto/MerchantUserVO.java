package com.payhub.merchant.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MerchantUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String merchantNo;

    private String username;

    private String phone;

    private String role;

    private String roleDesc;

    private Integer status;

    private String statusDesc;

    private String token;

    private LocalDateTime createdAt;
}
