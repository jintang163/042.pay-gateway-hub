package com.payhub.merchant.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class CallbackResendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "日志编号不能为空")
    private String logNo;
}
