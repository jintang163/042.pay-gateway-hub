package com.payhub.common.dto;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SignBaseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "商户号不能为空")
    private String merchantNo;

    @NotBlank(message = "时间戳不能为空")
    private String timestamp;

    @NotBlank(message = "随机串不能为空")
    private String nonce;

    @NotBlank(message = "签名类型不能为空")
    private String signType;

    @NotBlank(message = "签名不能为空")
    private String sign;
}
