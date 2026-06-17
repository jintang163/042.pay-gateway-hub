package com.payhub.settlement.dto;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplitRuleSaveRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "商户号不能为空")
    private String merchantNo;

    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    @NotBlank(message = "分账明细不能为空")
    private String splitDetails;

    private Integer status;
}
