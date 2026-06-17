package com.payhub.channel.config;

import com.payhub.channel.strategy.PayChannelStrategyFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.payhub.channel")
@EnableConfigurationProperties(ChannelProperties.class)
public class ChannelAutoConfiguration {

    @Bean
    public PayChannelStrategyFactory payChannelStrategyFactory() {
        return new PayChannelStrategyFactory();
    }
}
