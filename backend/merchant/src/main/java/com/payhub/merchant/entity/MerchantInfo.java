package com.payhub.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("merchant_info")
public class MerchantInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String merchantNo;

    private String merchantName;

    private String businessLicenseNo;

    private String legalPersonName;

    private String legalPersonIdNo;

    private String contactPhone;

    private String contactEmail;

    private String settlementBankName;

    private String settlementBankAccount;

    private String settlementAccountName;

    private Integer auditStatus;

    private String auditRemark;

    private Integer status;

    private String apiKeyMd5;

    private String apiKeyRsaPublic;

    private String apiKeyRsaPrivate;

    private String apiKeySm2Public;

    private String apiKeySm2Private;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
