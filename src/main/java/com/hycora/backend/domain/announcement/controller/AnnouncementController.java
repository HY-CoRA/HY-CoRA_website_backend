package com.hycora.backend.domain.announcement.controller;

import com.hycora.backend.domain.announcement.dto.AnnouncementDto;
import com.hycora.backend.domain.announcement.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Announcements", description = "공지사항 API")
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "공지사항 공개 목록 조회", description = "published=true인 공지사항만 반환합니다.")
    @GetMapping
    public ResponseEntity<?> getPublicList(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(announcementService.getPublicList(category, sort, page, limit));
    }

    @Operation(summary = "공지사항 공개 상세 조회", description = "published=true인 공지사항만 반환합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<?> getPublicOne(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(announcementService.getPublicOne(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "공지사항 생성", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<?> create(@RequestBody AnnouncementDto.Request req) {
        try {
            Long id = announcementService.create(req);
            return ResponseEntity.status(201).body(Map.of("id", String.valueOf(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "공지사항 수정", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody AnnouncementDto.Request req) {
        try {
            return ResponseEntity.ok(Map.of("id", String.valueOf(announcementService.update(id, req))));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            int status = msg.contains("찾을 수 없습니다") ? 404 : 400;
            return ResponseEntity.status(status).body(Map.of("error", msg));
        }
    }

    @Operation(summary = "공지사항 삭제", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            announcementService.delete(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "공지사항 발행 상태 토글", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}/publish")
    public ResponseEntity<?> togglePublish(@PathVariable Long id) {
        try {
            boolean published = announcementService.togglePublish(id);
            return ResponseEntity.ok(Map.of("published", published));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
