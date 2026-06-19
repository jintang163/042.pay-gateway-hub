package com.payhub.settlement.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class SplitReceiverSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String receiverNo;

    @NotBlank(message = "接收方名称不能为空")
    private String receiverName;

    @NotNull(message = "接收方类型不能为空")
    private Integer receiverType;

    @NotBlank(message = "证件号码不能为空")
    private String idCardNo;

    @NotBlank(message = "证件姓名不能为空")
    private String idCardName;

    @NotBlank(message = "银行卡号不能为空")
    private String bankCardNo;

    @NotBlank(message = "预留手机号不能为空")
    private String bankPhone;

    @NotBlank(message = "开户银行不能为空")
    private String bankName;

    private String bankBranchName;

    private String contactName;

    private String contactPhone;

    private String contactEmail;

    private String remark;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
