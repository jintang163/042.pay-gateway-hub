package com.payhub.invoice.strategy;

import com.payhub.common.exception.BusinessException;
import com.payhub.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class InvoiceChannelStrategyFactory {

    private final Map<String, InvoiceChannelStrategy> strategyMap = new ConcurrentHashMap<>();

    @Autowired
    public InvoiceChannelStrategyFactory(List<InvoiceChannelStrategy> strategies) {
        for (InvoiceChannelStrategy strategy : strategies) {
            strategyMap.put(strategy.getChannelCode().toUpperCase(), strategy);
            log.info("已注册发票渠道策略: {}", strategy.getChannelCode());
        }
    }

    public InvoiceChannelStrategy getStrategy(String channelCode) {
        if (channelCode == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "发票渠道代码不能为空");
        }
        InvoiceChannelStrategy strategy = strategyMap.get(channelCode.toUpperCase());
        if (strategy == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的发票渠道: " + channelCode);
        }
        return strategy;
    }

    public boolean hasStrategy(String channelCode) {
        if (channelCode == null) return false;
        return strategyMap.containsKey(channelCode.toUpperCase());
    }
}
