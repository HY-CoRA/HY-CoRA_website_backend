package com.hycora.backend.domain.announcement.controller;

import com.hycora.backend.domain.announcement.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Admin - Announcements", description = "공지사항 어드민 API")
@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "공지사항 어드민 전체 목록 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<?> getAdminList() {
        return ResponseEntity.ok(announcementService.getAdminList());
    }

    @Operation(summary = "공지사항 어드민 상세 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    public ResponseEntity<?> getAdminOne(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(announcementService.getAdminOne(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
