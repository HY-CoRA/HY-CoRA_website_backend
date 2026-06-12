package com.hycora.backend.domain.auth.controller;

import com.hycora.backend.domain.auth.dto.*;
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
        // Stateless JWT → 클라이언트에서 토큰 삭제
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ── Magic Link ────────────────────────────────────────────────

    @PostMapping("/magic-link/request")
    public ResponseEntity<?> requestMagicLink(@RequestBody MagicLinkRequestDto dto) {
        try {
            authService.requestMagicLink(dto.getEmail());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
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

    // ── WebAuthn Login ────────────────────────────────────────────

    @PostMapping("/webauthn/login/options")
    public ResponseEntity<?> loginOptions(@RequestBody WebAuthnLoginOptionsDto dto) {
        try {
            return ResponseEntity.ok(authService.getLoginOptions(dto.getEmail()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/webauthn/login/verify")
    public ResponseEntity<?> loginVerify(@RequestBody WebAuthnCredentialDto dto) {
        try {
            AuthResponseDto response = authService.verifyLogin(
                    dto.getEmail(),
                    dto.getCredential().getId()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // ── WebAuthn Register ─────────────────────────────────────────

    @PostMapping("/webauthn/register/options")
    public ResponseEntity<?> registerOptions(@RequestBody WebAuthnRegisterOptionsDto dto) {
        try {
            return ResponseEntity.ok(authService.getRegisterOptions(dto.getEmail(), dto.getDisplayName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/webauthn/register/verify")
    public ResponseEntity<?> registerVerify(@RequestBody WebAuthnCredentialDto dto) {
        try {
            authService.registerCredential(
                    dto.getEmail(),
                    dto.getCredential().getId(),
                    dto.getCredential().getResponse().getAttestationObject()
            );
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}
