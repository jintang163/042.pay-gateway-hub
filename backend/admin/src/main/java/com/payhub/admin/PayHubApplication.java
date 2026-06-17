package com.payhub.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.payhub")
@MapperScan("com.payhub.**.mapper")
@EnableScheduling
public class PayHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayHubApplication.class, args);
    }
}
