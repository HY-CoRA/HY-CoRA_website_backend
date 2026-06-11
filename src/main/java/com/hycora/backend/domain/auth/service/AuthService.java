package com.hycora.backend.domain.auth.service;

import com.hycora.backend.domain.admin.entity.Admin;
import com.hycora.backend.domain.admin.entity.MagicLinkToken;
import com.hycora.backend.domain.admin.repository.AdminRepository;
import com.hycora.backend.domain.admin.repository.MagicLinkTokenRepository;
import com.hycora.backend.domain.auth.dto.AuthResponseDto;
import com.hycora.backend.global.auth.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AdminRepository adminRepository;
    private final MagicLinkTokenRepository magicLinkTokenRepository;
    private final JwtProvider jwtProvider;
    private final JavaMailSender mailSender;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

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
}
