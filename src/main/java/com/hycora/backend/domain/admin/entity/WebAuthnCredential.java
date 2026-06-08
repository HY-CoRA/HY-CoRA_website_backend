package com.hycora.backend.domain.admin.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "webauthn_credential")
@Getter
@NoArgsConstructor
public class WebAuthnCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @Column(name = "credential_id", nullable = false, unique = true, length = 512)
    private String credentialId;

    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "sign_count", nullable = false)
    private long signCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public WebAuthnCredential(Admin admin, String credentialId, String publicKey, long signCount) {
        this.admin = admin;
        this.credentialId = credentialId;
        this.publicKey = publicKey;
        this.signCount = signCount;
        this.createdAt = LocalDateTime.now();
    }

    public void updateSignCount(long signCount) {
        this.signCount = signCount;
    }
}
