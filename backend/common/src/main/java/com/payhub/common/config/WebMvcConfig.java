package com.payhub.common.config;

import com.payhub.common.interceptor.ApiLogInterceptor;
import com.payhub.common.interceptor.SignInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final SignInterceptor signInterceptor;

    private final ApiLogInterceptor apiLogInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiLogInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/error",
                        "/static/**",
                        "/public/**",
                        "/actuator/**",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**"
                )
                .order(1);

        registry.addInterceptor(signInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/**/notify",
                        "/api/**/callback"
                )
                .order(2);
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
}
