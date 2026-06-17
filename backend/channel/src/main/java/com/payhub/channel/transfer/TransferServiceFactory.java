package com.payhub.channel.transfer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TransferServiceFactory implements ApplicationContextAware {

    private static final Map<String, TransferService> SERVICE_MAP = new ConcurrentHashMap<>();

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        initServices();
    }

    private void initServices() {
        Map<String, TransferService> beans = applicationContext.getBeansOfType(TransferService.class);
        for (TransferService service : beans.values()) {
            String channelCode = service.getChannelCode();
            SERVICE_MAP.put(channelCode, service);
            log.info("加载转账通道服务: {}", channelCode);
        }
    }

    public TransferService getTransferService(String channelCode) {
        TransferService service = SERVICE_MAP.get(channelCode);
        if (service == null) {
            throw new IllegalArgumentException("不支持的转账通道: " + channelCode);
        }
        return service;
    }

    public void registerTransferService(String channelCode, TransferService service) {
        SERVICE_MAP.put(channelCode, service);
        log.info("注册转账通道服务: {}", channelCode);
    }

    public boolean containsTransferService(String channelCode) {
        return SERVICE_MAP.containsKey(channelCode);
    }
}
