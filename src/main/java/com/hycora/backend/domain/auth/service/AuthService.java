package com.hycora.backend.domain.auth.service;

import com.hycora.backend.domain.admin.entity.Admin;
import com.hycora.backend.domain.admin.entity.MagicLinkToken;
import com.hycora.backend.domain.admin.entity.WebAuthnCredential;
import com.hycora.backend.domain.admin.repository.AdminRepository;
import com.hycora.backend.domain.admin.repository.MagicLinkTokenRepository;
import com.hycora.backend.domain.admin.repository.WebAuthnCredentialRepository;
import com.hycora.backend.domain.auth.dto.AuthResponseDto;
import com.hycora.backend.global.auth.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AdminRepository adminRepository;
    private final MagicLinkTokenRepository magicLinkTokenRepository;
    private final WebAuthnCredentialRepository webAuthnCredentialRepository;
    private final JwtProvider jwtProvider;
    private final JavaMailSender mailSender;

    // WebAuthn challenge를 임시 저장 (운영 환경에서는 Redis 등 사용 권장)
    private final Map<String, String> challengeStore = new HashMap<>();

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.rp-id:localhost}")
    private String rpId;

    @Value("${app.rp-name:HY-CoRA}")
    private String rpName;

    // ── Magic Link ────────────────────────────────────────────────

    @Transactional
    public void requestMagicLink(String email) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        String token = UUID.randomUUID().toString();
        MagicLinkToken magicLink = MagicLinkToken.builder()
                .token(token)
                .admin(admin)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        magicLinkTokenRepository.save(magicLink);

        sendMagicLinkEmail(email, token);
    }

    private void sendMagicLinkEmail(String to, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("[HY-CoRA] 관리자 로그인 링크");
            message.setText("아래 링크로 로그인하세요 (15분 내 사용):\n\n" +
                    baseUrl + "/admin/auth/callback?token=" + token + "\n\n" +
                    "본인이 요청하지 않았다면 무시하세요.");
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("Magic link 이메일 발송 실패: {}", e.getMessage());
        }
    }

    @Transactional
    public AuthResponseDto verifyMagicLink(String token) {
        MagicLinkToken magicLink = magicLinkTokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        if (magicLink.isExpired()) {
            throw new IllegalArgumentException("Token has expired");
        }

        magicLink.markUsed();
        Admin admin = magicLink.getAdmin();
        return buildAuthResponse(admin);
    }

    // ── WebAuthn ─────────────────────────────────────────────────

    public Map<String, Object> getLoginOptions(String email) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        String challenge = generateChallenge();
        challengeStore.put(email + ":login", challenge);

        List<WebAuthnCredential> credentials = webAuthnCredentialRepository
                .findAllByAdmin_AdminId(admin.getAdminId());

        List<Map<String, String>> allowCredentials = credentials.stream()
                .map(c -> Map.of("id", c.getCredentialId(), "type", "public-key"))
                .toList();

        return Map.of(
                "challenge", challenge,
                "rpId", rpId,
                "allowCredentials", allowCredentials,
                "userVerification", "preferred",
                "timeout", 60000
        );
    }

    @Transactional
    public AuthResponseDto verifyLogin(String email, String credentialId) {
        WebAuthnCredential credential = webAuthnCredentialRepository
                .findByCredentialId(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("Credential not found"));

        // 실제 WebAuthn 서명 검증은 webauthn4j 라이브러리로 처리 필요
        // 여기서는 credential 존재 여부로 1차 확인
        Admin admin = credential.getAdmin();
        if (!admin.getEmail().equals(email)) {
            throw new IllegalArgumentException("Credential does not match email");
        }

        return buildAuthResponse(admin);
    }

    public Map<String, Object> getRegisterOptions(String email, String displayName) {
        adminRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        String challenge = generateChallenge();
        challengeStore.put(email + ":register", challenge);

        return Map.of(
                "challenge", challenge,
                "rp", Map.of("id", rpId, "name", rpName),
                "user", Map.of(
                        "id", Base64.getEncoder().encodeToString(email.getBytes()),
                        "name", email,
                        "displayName", displayName
                ),
                "pubKeyCredParams", List.of(
                        Map.of("type", "public-key", "alg", -7),   // ES256
                        Map.of("type", "public-key", "alg", -257)  // RS256
                ),
                "authenticatorSelection", Map.of("userVerification", "preferred"),
                "timeout", 60000
        );
    }

    @Transactional
    public void registerCredential(String email, String credentialId, String publicKey) {
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        WebAuthnCredential credential = WebAuthnCredential.builder()
                .admin(admin)
                .credentialId(credentialId)
                .publicKey(publicKey)
                .signCount(0)
                .build();
        webAuthnCredentialRepository.save(credential);
    }

    // ── /auth/me ─────────────────────────────────────────────────

    public AuthResponseDto.UserDto getMe(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        return AuthResponseDto.UserDto.builder()
                .id(String.valueOf(admin.getAdminId()))
                .email(admin.getEmail())
                .role(admin.getRole().toLower())
                .build();
    }

    // ── 공통 ─────────────────────────────────────────────────────

    private AuthResponseDto buildAuthResponse(Admin admin) {
        String token = jwtProvider.generate(
                admin.getAdminId(),
                admin.getEmail(),
                admin.getRole().toLower()
        );
        return AuthResponseDto.builder()
                .token(token)
                .user(AuthResponseDto.UserDto.builder()
                        .id(String.valueOf(admin.getAdminId()))
                        .email(admin.getEmail())
                        .role(admin.getRole().toLower())
                        .build())
                .build();
    }

    private String generateChallenge() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
