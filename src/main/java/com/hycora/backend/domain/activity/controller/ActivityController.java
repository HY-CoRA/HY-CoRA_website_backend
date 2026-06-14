package com.hycora.backend.domain.activity.controller;

import com.hycora.backend.domain.activity.dto.ActivityDto;
import com.hycora.backend.domain.activity.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(activityService.getAll(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(activityService.getOne(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ActivityDto.Request req) {
        try {
            return ResponseEntity.status(201).body(Map.of("id", String.valueOf(activityService.create(req))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ActivityDto.Request req) {
        try {
            return ResponseEntity.ok(Map.of("id", String.valueOf(activityService.update(id, req))));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            int status = msg.contains("not found") ? 404 : 400;
            return ResponseEntity.status(status).body(Map.of("error", msg));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            activityService.delete(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
