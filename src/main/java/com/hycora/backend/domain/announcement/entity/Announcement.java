package com.hycora.backend.domain.announcement.entity;

import com.hycora.backend.domain.admin.entity.Admin;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    public static Announcement create(String category, String title, String summary, String content,
                                      String date, Boolean published, String source,
                                      String capacity, String link) {
        Announcement a = new Announcement();
        a.category = category;
        a.categoryKo = toCategoryKo(category);
        a.title = title;
        a.summary = summary;
        a.content = content;
        a.date = date;
        a.published = published != null ? published : false;
        a.source = source != null ? source : "manual";
        a.capacity = capacity;
        a.link = link;
        a.lastModified = now();
        return a;
    }

    public void update(String category, String title, String summary, String content,
                       String date, Boolean published, String source,
                       String capacity, String link) {
        this.category = category;
        this.categoryKo = toCategoryKo(category);
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.date = date;
        this.published = published != null ? published : false;
        this.source = source != null ? source : "manual";
        this.capacity = capacity;
        this.link = link;
        this.lastModified = now();
    }

    public void togglePublish() {
        this.published = !Boolean.TRUE.equals(this.published);
        this.lastModified = now();
    }

    private static String toCategoryKo(String category) {
        if (category == null) return null;
        return switch (category) {
            case "event" -> "행사";
            case "recruitment" -> "모집";
            case "etc" -> "기타";
            default -> category;
        };
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }
}
