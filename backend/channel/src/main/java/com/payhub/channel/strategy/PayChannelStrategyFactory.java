package com.payhub.channel.strategy;

import com.payhub.channel.enums.PayChannelEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PayChannelStrategyFactory implements ApplicationContextAware {

    private static final Map<String, PayChannelStrategy> STRATEGY_MAP = new ConcurrentHashMap<>();

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        initStrategies();
    }

    private void initStrategies() {
        Map<String, PayChannelStrategy> beans = applicationContext.getBeansOfType(PayChannelStrategy.class);
        for (PayChannelStrategy strategy : beans.values()) {
            if (strategy instanceof AbstractPayChannel) {
                String channelCode = ((AbstractPayChannel) strategy).getChannelCode();
                STRATEGY_MAP.put(channelCode, strategy);
                log.info("加载支付通道策略: {}", channelCode);
            }
        }
    }

    public PayChannelStrategy getStrategy(String channelCode) {
        PayChannelStrategy strategy = STRATEGY_MAP.get(channelCode);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的支付通道: " + channelCode);
        }
        return strategy;
    }

    public PayChannelStrategy getStrategy(PayChannelEnum channelEnum) {
        if (channelEnum == null) {
            throw new IllegalArgumentException("支付通道枚举不能为空");
        }
        return getStrategy(channelEnum.getCode());
    }

    public void registerStrategy(String channelCode, PayChannelStrategy strategy) {
        STRATEGY_MAP.put(channelCode, strategy);
        log.info("注册支付通道策略: {}", channelCode);
    }

    public boolean containsStrategy(String channelCode) {
        return STRATEGY_MAP.containsKey(channelCode);
    }
}
