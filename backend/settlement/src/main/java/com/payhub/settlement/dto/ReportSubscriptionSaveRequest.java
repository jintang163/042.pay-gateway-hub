package com.payhub.settlement.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class ReportSubscriptionSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String merchantNo;

    @NotNull(message = "报表类型不能为空")
    private Integer reportType;

    private String reportCategory;

    @NotNull(message = "推送渠道不能为空")
    private Integer pushChannel;

    private String emailList;

    private String phoneList;

    @NotBlank(message = "推送时间不能为空")
    private String pushTime;

    private Integer enabled;

    private String remark;
}
