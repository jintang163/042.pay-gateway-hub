package com.payhub.admin.config;

import com.payhub.common.interceptor.JwtAuthInterceptor;
import com.payhub.common.interceptor.SignInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final SignInterceptor signInterceptor;
    private final JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(signInterceptor)
                .addPathPatterns(
                        "/api/pay/unifiedorder",
                        "/api/pay/query",
                        "/api/pay/refund/apply",
                        "/api/pay/refund/query"
                );

        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns(
                        "/api/merchant/**",
                        "/api/pay/order/**",
                        "/api/pay/config/**",
                        "/api/risk/**",
                        "/api/api-stats/**",
                        "/api/sandbox/**",
                        "/api/settlement/**",
                        "/api/split-rule/**",
                        "/api/reconcile/**"
                )
                .excludePathPatterns(
                        "/api/merchant/user/login",
                        "/api/merchant/user/register",
                        "/api/merchant/apply",
                        "/api/pay/notify/**"
                );
    }
}
