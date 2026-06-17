package com.payhub.pay.dto;

import com.payhub.common.dto.SignBaseDTO;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderQueryRequest extends SignBaseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private String merchantOrderNo;
}
