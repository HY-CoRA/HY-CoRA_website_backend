package com.hycora.backend.domain.admin.repository;

import com.hycora.backend.domain.admin.entity.WebAuthnCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, Long> {
    List<WebAuthnCredential> findAllByAdmin_AdminId(Long adminId);
    Optional<WebAuthnCredential> findByCredentialId(String credentialId);
}
