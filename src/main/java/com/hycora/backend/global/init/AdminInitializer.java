package com.hycora.backend.global.init;

import com.hycora.backend.domain.admin.entity.Admin;
import com.hycora.backend.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements ApplicationRunner {

    private final AdminRepository adminRepository;

    @Value("${app.admin.initial-email:}")
    private String initialEmail;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(initialEmail)) {
            log.info("ADMIN_INITIAL_EMAIL 미설정 — 초기 관리자 계정 생성 생략");
            return;
        }

        if (adminRepository.count() > 0) {
            log.info("관리자 계정이 이미 존재합니다 — 초기 계정 생성 생략");
            return;
        }

        Admin admin = Admin.builder()
                .email(initialEmail)
                .role(Admin.Role.OWNER)
                .build();
        adminRepository.save(admin);
        log.info("초기 관리자 계정 생성 완료: {}", initialEmail);
    }
}
