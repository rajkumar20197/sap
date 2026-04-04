package com.sap.sflit.config;

import com.sap.sflit.multitenancy.TenantInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration — Registers interceptors and CORS settings.
 *
 * INTERVIEW NOTE:
 * WebMvcConfigurer is the modern way (Spring 5+) to customize MVC behavior
 * without extending WebMvcConfigurationSupport (which disables auto-configuration).
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**") // Only apply to API routes
                .excludePathPatterns("/actuator/**", "/health");
    }
}
