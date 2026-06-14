package com.hycora.backend.domain.siteconfig.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
    name = "site_config",
    uniqueConstraints = @UniqueConstraint(name = "uk_site_config_key", columnNames = {"`key`"})
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "`key`", nullable = false, length = 200)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDate.now();
    }

    public void updateValue(String value) {
        this.value = value;
    }
}
