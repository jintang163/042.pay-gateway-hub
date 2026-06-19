package com.payhub.settlement.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class SplitReceiverVerifyRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "接收方编号不能为空")
    private String receiverNo;

    @NotNull(message = "认证渠道不能为空")
    private Integer verifyChannel;
}
