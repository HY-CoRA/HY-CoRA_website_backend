package com.hycora.backend.domain.pastevent.controller;

import com.hycora.backend.domain.pastevent.dto.PastEventDto;
import com.hycora.backend.domain.pastevent.service.PastEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/events/past")
@RequiredArgsConstructor
public class PastEventController {

    private final PastEventService pastEventService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(pastEventService.getAll());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PastEventDto.Request req) {
        try {
            return ResponseEntity.status(201).body(pastEventService.create(req));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/reorder")
    public ResponseEntity<?> reorder(@RequestBody PastEventDto.ReorderRequest req) {
        try {
            pastEventService.reorder(req.getIds());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody PastEventDto.Request req) {
        try {
            return ResponseEntity.ok(pastEventService.update(id, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            pastEventService.delete(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
