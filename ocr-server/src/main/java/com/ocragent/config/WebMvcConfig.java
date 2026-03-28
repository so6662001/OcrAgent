package com.ocragent.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept", "Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public Filter securityHeadersFilter() {
        return (request, response, chain) -> {
            if (response instanceof HttpServletResponse httpResp) {
                httpResp.setHeader("X-Content-Type-Options", "nosniff");
                httpResp.setHeader("X-Frame-Options", "DENY");
                httpResp.setHeader("X-XSS-Protection", "1; mode=block");
                httpResp.setHeader("Cache-Control", "no-store");
                httpResp.setHeader("Content-Security-Policy", "default-src 'self'");
                httpResp.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            }
            chain.doFilter(request, response);
        };
    }
}
