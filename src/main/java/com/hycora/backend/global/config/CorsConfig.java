package com.hycora.backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.origin}")
    private String corsOrigin;

    private final Environment environment;

    public CorsConfig(Environment environment) {
        this.environment = environment;
    }

    // SecurityConfig의 http.cors()가 이 Bean을 사용한다.
    // 인증이 필요한 API의 preflight(OPTIONS)도 Security 필터 안에서 CORS 처리되도록 하기 위함.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> allowedOrigins = new ArrayList<>();
        allowedOrigins.add(corsOrigin);

        // 개발 환경에서만 로컬 origin 허용
        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev")
                || Arrays.stream(environment.getActiveProfiles()).findAny().isEmpty();

        if (isDev) {
            allowedOrigins.addAll(List.of(
                    "http://localhost:3000",
                    "http://localhost:5173",
                    "http://localhost:5500",
                    "http://127.0.0.1:5500"
            ));
        }

        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
