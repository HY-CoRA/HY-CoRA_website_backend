package com.hycora.backend.domain.auth.controller;

import com.hycora.backend.domain.auth.dto.AuthResponseDto;
import com.hycora.backend.domain.auth.dto.MagicLinkRequestDto;
import com.hycora.backend.domain.auth.dto.MagicLinkVerifyDto;
import com.hycora.backend.domain.auth.service.AuthService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ── /api/auth/me ─────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        Claims claims = (Claims) authentication.getPrincipal();
        Long adminId = Long.parseLong(claims.getSubject());
        return ResponseEntity.ok(Map.of("user", authService.getMe(adminId)));
    }

    // ── /api/auth/logout ─────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── Magic Link ────────────────────────────────────────────────

    @PostMapping("/magic-link/request")
    public ResponseEntity<?> requestMagicLink(@RequestBody MagicLinkRequestDto dto) {
        try {
            authService.requestMagicLink(dto.getEmail());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/magic-link/verify")
    public ResponseEntity<?> verifyMagicLink(@RequestBody MagicLinkVerifyDto dto) {
        try {
            AuthResponseDto response = authService.verifyMagicLink(dto.getToken());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
}
