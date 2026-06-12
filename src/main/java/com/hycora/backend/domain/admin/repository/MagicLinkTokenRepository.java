package com.hycora.backend.domain.admin.repository;

import com.hycora.backend.domain.admin.entity.MagicLinkToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MagicLinkTokenRepository extends JpaRepository<MagicLinkToken, Long> {
    Optional<MagicLinkToken> findByTokenAndUsedFalse(String token);
}
