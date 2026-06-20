package com.payhub.marketing.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class MerchantAdSaveRequest {

    private Long id;

    @NotBlank(message = "商户号不能为空")
    private String merchantNo;

    @NotBlank(message = "广告标题不能为空")
    @Size(max = 100, message = "广告标题最多100字")
    private String adTitle;

    @Size(max = 500, message = "广告描述最多500字")
    private String adDescription;

    @Size(max = 500, message = "广告图片URL过长")
    private String adImageUrl;

    @NotBlank(message = "跳转链接不能为空")
    @Size(max = 500, message = "跳转链接过长")
    private String targetUrl;

    @NotBlank(message = "展示位置不能为空")
    private String position;

    @DecimalMin(value = "0.0000", message = "点击单价不能小于0")
    @DecimalMax(value = "1000.0000", message = "点击单价不能超过1000元")
    private BigDecimal cpcPrice;

    @Min(value = 0, message = "排序值不能小于0")
    @Max(value = 999, message = "排序值不能超过999")
    private Integer sortOrder;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String startTime;

    private String endTime;

    @DecimalMin(value = "0.0000", message = "每日预算不能小于0")
    private BigDecimal dailyBudget;

    @Size(max = 500, message = "备注最多500字")
    private String remark;
}
