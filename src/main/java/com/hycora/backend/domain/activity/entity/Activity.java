package com.hycora.backend.domain.activity.entity;

import com.hycora.backend.domain.admin.entity.Admin;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
    name = "activities",
    indexes = {
        @Index(name = "idx_activities_status", columnList = "status"),
        @Index(name = "idx_activities_recruit_end", columnList = "recruit_end")
    }
)
@Getter
@NoArgsConstructor
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String status;

    @Column(name = "status_label", length = 20)
    private String statusLabel;

    @Column(length = 200)
    private String title;

    @Column(name = "`desc`", length = 255)
    private String desc;

    @Column(columnDefinition = "TEXT")
    private String intro;

    @Column(length = 100)
    private String place;

    @Column(length = 100)
    private String mentor;

    @Column(length = 100)
    private String role;

    @Column(length = 50)
    private String phone;

    @Column(length = 255)
    private String participants;

    @Column(name = "recruit_start")
    private LocalDate recruitStart;

    @Column(name = "recruit_end")
    private LocalDate recruitEnd;

    @Column(name = "period_text", length = 100)
    private String periodText;

    @Column(columnDefinition = "TEXT")
    private String images;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

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
