package com.payhub.pay.dto.vo;

import com.payhub.channel.entity.PayChannelLog;
import com.payhub.risk.entity.RiskControlLog;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class OrderAttributionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private String failCode;

    private String failMessage;

    private String failCategory;

    private String suggestion;

    private String ruleDescription;

    private Integer priority;

    private List<String> evidence;

    private PayOrderBriefVO orderInfo;

    private PayChannelLog latestChannelLog;

    private RiskControlLog latestRiskLog;
}
