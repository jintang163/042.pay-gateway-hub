package com.payhub.common.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BasePageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码最小为1")
    @Builder.Default
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    @Builder.Default
    private Integer pageSize = 10;

    private String orderBy;

    private String orderDirection;

    public long getOffset() {
        return (long) (pageNum - 1) * pageSize;
    }
}
