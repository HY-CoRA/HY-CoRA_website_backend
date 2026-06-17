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
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SiteConfigService {

    private final SiteConfigRepository siteConfigRepository;
    private final ObjectMapper objectMapper;

    private static final java.util.Set<String> VALID_KEYS =
            java.util.Set.of("main-banner", "about-banner", "apply-links", "recruitment-schedule");

    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private static final java.util.Map<String, Map<String, Object>> DEFAULTS = java.util.Map.of(
            "main-banner", java.util.Map.of("imageUrl", "", "altText", "HY-CoRA 메인 배너"),
            "about-banner", java.util.Map.of("imageUrl", "", "altText", "HY-CoRA 소개 페이지 배너"),
            "apply-links", java.util.Map.of(
                    "newMember", java.util.Map.of("url", "", "label", "신규 지원"),
                    "returning", java.util.Map.of("url", "", "label", "재가입 지원")
            )
    );

    public Map<String, Object> get(String key) {
        validateKey(key);
        if ("recruitment-schedule".equals(key)) {
            return getRecruitmentSchedule();
        }
        return siteConfigRepository.findByKey(key)
                .map(config -> parseValue(key, config.getValue()))
                .orElse(DEFAULTS.get(key));
    }

    @Transactional
    public void save(String key, Map<String, Object> body) {
        validateKey(key);
        if ("recruitment-schedule".equals(key)) {
            body = sanitizeRecruitmentSchedule(body);
        }
        String json = toJson(body);
        siteConfigRepository.findByKey(key).ifPresentOrElse(
                config -> config.updateValue(json),
                () -> siteConfigRepository.save(
                        SiteConfig.builder().key(key).value(json).build()
                )
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sanitizeRecruitmentSchedule(Map<String, Object> body) {
        Map<String, Object> result = new java.util.HashMap<>();
        for (String semester : java.util.List.of("semester1", "semester2")) {
            Object raw = body.get(semester);
            Map<String, Object> semesterData = new java.util.HashMap<>();
            if (raw instanceof Map) {
                Map<String, Object> semesterMap = (Map<String, Object>) raw;
                semesterData.put("regularStart", validateDate(semester + ".regularStart", semesterMap.get("regularStart")));
                semesterData.put("regularEnd", validateDate(semester + ".regularEnd", semesterMap.get("regularEnd")));
            } else {
                semesterData.put("regularStart", null);
                semesterData.put("regularEnd", null);
            }
            result.put(semester, semesterData);
        }
        return result;
    }

    private Object validateDate(String fieldName, Object value) {
        if (value == null) return null;
        if (!(value instanceof String)) {
            throw new IllegalArgumentException(fieldName + " 값은 null 또는 yyyy-MM-dd 형식이어야 합니다.");
        }
        String dateStr = (String) value;
        if (!DATE_PATTERN.matcher(dateStr).matches()) {
            throw new IllegalArgumentException(fieldName + " 날짜 형식이 올바르지 않습니다: " + dateStr + " (yyyy-MM-dd 형식 필요)");
        }
        return dateStr;
    }

    private Map<String, Object> getRecruitmentSchedule() {
        int year = siteConfigRepository.findByKey("recruitment-year")
                .map(c -> Integer.parseInt(c.getValue()))
                .orElse(java.time.Year.now().getValue());

        Map<String, Object> schedule = siteConfigRepository.findByKey("recruitment-schedule")
                .map(c -> {
                    try {
                        return objectMapper.readValue(c.getValue(), new TypeReference<Map<String, Object>>() {});
                    } catch (JsonProcessingException e) {
                        return new java.util.HashMap<String, Object>();
                    }
                })
                .orElse(new java.util.HashMap<>());

        Map<String, Object> s1 = getOrEmptySemester(schedule, "semester1");
        Map<String, Object> s2 = getOrEmptySemester(schedule, "semester2");
        s1.put("reregistrationDate", "02.15");
        s2.put("reregistrationDate", "08.15");

        return java.util.Map.of("year", year, "semester1", s1, "semester2", s2);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOrEmptySemester(Map<String, Object> schedule, String key) {
        Object raw = schedule.get(key);
        if (raw instanceof Map) {
            return new java.util.HashMap<>((Map<String, Object>) raw);
        }
        Map<String, Object> empty = new java.util.HashMap<>();
        empty.put("regularStart", null);
        empty.put("regularEnd", null);
        return empty;
    }

    @Transactional
    public void updateRecruitmentYear(int newYear) {
        upsert("recruitment-year", String.valueOf(newYear));

        // 정규 모집 날짜 초기화
        Map<String, Object> schedule = siteConfigRepository.findByKey("recruitment-schedule")
                .map(c -> {
                    try {
                        return objectMapper.readValue(c.getValue(), new TypeReference<Map<String, Object>>() {});
                    } catch (JsonProcessingException e) {
                        return new java.util.HashMap<String, Object>();
                    }
                })
                .orElse(new java.util.HashMap<>());

        Map<String, Object> empty = new java.util.HashMap<>();
        empty.put("regularStart", null);
        empty.put("regularEnd", null);
        schedule.put("semester1", new java.util.HashMap<>(empty));
        schedule.put("semester2", new java.util.HashMap<>(empty));
        upsert("recruitment-schedule", toJson(schedule));
    }

    public int getCurrentRecruitmentYear() {
        return siteConfigRepository.findByKey("recruitment-year")
                .map(c -> Integer.parseInt(c.getValue()))
                .orElse(java.time.Year.now().getValue());
    }

    private void upsert(String key, String value) {
        siteConfigRepository.findByKey(key).ifPresentOrElse(
                config -> config.updateValue(value),
                () -> siteConfigRepository.save(SiteConfig.builder().key(key).value(value).build())
        );
    }

    private void validateKey(String key) {
        if (!VALID_KEYS.contains(key)) {
            throw new IllegalArgumentException("Invalid config key: " + key);
        }
    }

    private Map<String, Object> parseValue(String key, String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            // 단순 URL 문자열인 경우 imageUrl에 넣고 altText는 기본값 사용
            Map<String, Object> defaults = DEFAULTS.get(key);
            return java.util.Map.of("imageUrl", json, "altText", defaults.getOrDefault("altText", ""));
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
