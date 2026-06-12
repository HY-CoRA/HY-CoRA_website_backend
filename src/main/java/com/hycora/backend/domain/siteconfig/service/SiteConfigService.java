package com.hycora.backend.domain.siteconfig.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hycora.backend.domain.siteconfig.entity.SiteConfig;
import com.hycora.backend.domain.siteconfig.repository.SiteConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SiteConfigService {

    private final SiteConfigRepository siteConfigRepository;
    private final ObjectMapper objectMapper;

    private static final java.util.Set<String> VALID_KEYS =
            java.util.Set.of("main-banner", "about-banner", "apply-links");

    public Map<String, Object> get(String key) {
        validateKey(key);
        SiteConfig config = siteConfigRepository.findByKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Config not found: " + key));
        return parseValue(config.getValue());
    }

    @Transactional
    public void save(String key, Map<String, Object> body) {
        validateKey(key);
        String json = toJson(body);
        siteConfigRepository.findByKey(key).ifPresentOrElse(
                config -> config.updateValue(json),
                () -> siteConfigRepository.save(
                        SiteConfig.builder().key(key).value(json).build()
                )
        );
    }

    private void validateKey(String key) {
        if (!VALID_KEYS.contains(key)) {
            throw new IllegalArgumentException("Invalid config key: " + key);
        }
    }

    private Map<String, Object> parseValue(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse config value");
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize config value");
        }
    }
}
