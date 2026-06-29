package com.hycora.backend.global.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final List<String> UPLOAD_SUBDIRECTORIES =
            List.of("banners", "leaders", "activities", "events");

    private final String uploadDir;

    public WebMvcConfig(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @PostConstruct
    public void init() {
        Path uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        for (String subdirectory : UPLOAD_SUBDIRECTORIES) {
            Path directory = uploadRoot.resolve(subdirectory);
            try {
                Files.createDirectories(directory);
            } catch (IOException exception) {
                throw new IllegalStateException("업로드 디렉터리를 생성할 수 없습니다: " + directory, exception);
            }
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
