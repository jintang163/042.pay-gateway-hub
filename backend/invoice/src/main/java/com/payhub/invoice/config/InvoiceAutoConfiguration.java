package com.payhub.invoice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@ComponentScan(basePackages = "com.payhub.invoice")
@EnableConfigurationProperties(InvoiceProperties.class)
@EnableAsync
public class InvoiceAutoConfiguration {
}
