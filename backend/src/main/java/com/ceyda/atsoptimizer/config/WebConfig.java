package com.ceyda.atsoptimizer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    private final String[] allowedOrigins;
    private final String[] allowedOriginPatterns;

    public WebConfig(
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}") String allowedOrigins,
            @Value("${app.cors.allowed-origin-patterns:https://*.vercel.app}") String allowedOriginPatterns) {
        this.allowedOrigins = split(allowedOrigins);
        this.allowedOriginPatterns = split(allowedOriginPatterns);
        log.info("CORS allowed origins: {}", Arrays.toString(this.allowedOrigins));
        log.info("CORS allowed origin patterns: {}", Arrays.toString(this.allowedOriginPatterns));
    }

    private String[] split(String value) {
        if (value == null || value.isBlank()) {
            return new String[0];
        }
        List<String> values = Arrays.stream(value.split("\\s*,\\s*"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        return values.toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedOriginPatterns(allowedOriginPatterns)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders(
                        "Accept",
                        "Authorization",
                        "Content-Type",
                        "Origin",
                        "X-Requested-With"
                )
                .exposedHeaders("Content-Disposition")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
