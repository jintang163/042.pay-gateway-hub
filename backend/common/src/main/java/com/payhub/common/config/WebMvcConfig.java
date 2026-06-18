package com.payhub.common.config;

import com.payhub.common.interceptor.ApiLogInterceptor;
import com.payhub.common.interceptor.JwtAuthInterceptor;
import com.payhub.common.interceptor.SignInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final SignInterceptor signInterceptor;

    private final JwtAuthInterceptor jwtAuthInterceptor;

    private final ApiLogInterceptor apiLogInterceptor;

    @Value("${payhub.upload.path:./uploads}")
    private String uploadPath;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiLogInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/error",
                        "/static/**",
                        "/public/**",
                        "/uploads/**",
                        "/actuator/**",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**"
                )
                .order(1);

        registry.addInterceptor(signInterceptor)
                .addPathPatterns(
                        "/api/pay/unifiedorder",
                        "/api/pay/query",
                        "/api/pay/refund/apply",
                        "/api/pay/refund/query"
                )
                .order(2);

        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns(
                        "/api/merchant/**",
                        "/api/pay/order/**",
                        "/api/pay/refund/list",
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
                        "/api/pay/notify/**",
                        "/api/pay/callback/**"
                )
                .order(3);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
