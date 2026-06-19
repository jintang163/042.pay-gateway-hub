package com.payhub.marketing.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class PayLinkSaveRequest {

    private Long id;

    @NotBlank(message = "商户号不能为空")
    private String merchantNo;

    @NotBlank(message = "链接标题不能为空")
    @Size(max = 100, message = "标题最多100字")
    private String title;

    private BigDecimal fixedAmount;

    private Boolean amountEditable = false;

    private BigDecimal minAmount;

    private BigDecimal maxAmount;

    private String payChannel;

    @Size(max = 200, message = "商品描述最多200字")
    private String productSubject;

    @Size(max = 500, message = "商品详情最多500字")
    private String productDetail;

    private String notifyUrl;

    private String redirectUrl;

    private String expireTime;

    private Boolean singleUse = false;

    private Integer maxUseCount;

    private String remark;
}
