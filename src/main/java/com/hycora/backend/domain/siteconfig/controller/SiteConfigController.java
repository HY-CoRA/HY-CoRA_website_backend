package com.hycora.backend.domain.siteconfig.controller;

import com.hycora.backend.domain.siteconfig.service.SiteConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class SiteConfigController {

    private final SiteConfigService siteConfigService;

    @GetMapping("/{key}")
    public ResponseEntity<?> get(@PathVariable String key) {
        try {
            return ResponseEntity.ok(siteConfigService.get(key));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{key}")
    public ResponseEntity<?> save(@PathVariable String key, @RequestBody Map<String, Object> body) {
        try {
            siteConfigService.save(key, body);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}
