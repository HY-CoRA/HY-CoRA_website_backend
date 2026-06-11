package com.hycora.backend.domain.announcement.entity;

import com.hycora.backend.domain.admin.entity.Admin;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
    name = "announcements",
    indexes = {
        @Index(name = "idx_announcements_category", columnList = "category"),
        @Index(name = "idx_announcements_published", columnList = "published"),
        @Index(name = "idx_announcements_created_at", columnList = "created_at")
    }
)
@Getter
@NoArgsConstructor
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String category;

    @Column(length = 200)
    private String title;

    @Column(length = 255)
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 100)
    private String date;

    @Column(columnDefinition = "TINYINT(1)")
    private Boolean published;

    @Column(length = 20)
    private String source;

    @Column(length = 100)
    private String capacity;

    @Column(length = 255)
    private String link;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    @Column(name = "category_ko", length = 20)
    private String categoryKo;

    @Column(name = "last_modified", length = 100)
    private String lastModified;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Admin admin;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDate.now();
        this.updatedAt = LocalDate.now();
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDate.now();
    }
}
