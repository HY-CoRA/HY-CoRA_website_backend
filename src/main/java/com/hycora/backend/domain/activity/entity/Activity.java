package com.hycora.backend.domain.activity.entity;

import jakarta.persistence.*;
import lombok.*;

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
@AllArgsConstructor
@Builder
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

    @Column(length = 255)
    private String participants;

    @Column(length = 50)
    private String phone;

    @Column(name = "recruit_start")
    private LocalDate recruitStart;

    @Column(name = "recruit_end")
    private LocalDate recruitEnd;

    @Column(name = "period_text", length = 100)
    private String periodText;

    @Column(columnDefinition = "TEXT")
    private String schedule;

    @Column(columnDefinition = "TEXT")
    private String images;

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

    public void updateStatus(String status, String statusLabel) {
        this.status = status;
        this.statusLabel = statusLabel;
    }

    public void updateImages(String images) {
        this.images = images;
    }

    public void update(String status, String title, String desc, String intro,
                       String mentor, String role, String place, String participants, String phone,
                       LocalDate recruitStart, LocalDate recruitEnd, String periodText,
                       String schedule, String images) {
        this.status = status;
        this.statusLabel = StatusLabel.from(status);
        this.title = title;
        this.desc = desc;
        this.intro = intro;
        this.mentor = mentor;
        this.role = role;
        this.place = place;
        this.participants = participants;
        this.phone = phone;
        this.recruitStart = recruitStart;
        this.recruitEnd = recruitEnd;
        this.periodText = periodText;
        this.schedule = schedule;
        this.images = images;
    }
}
