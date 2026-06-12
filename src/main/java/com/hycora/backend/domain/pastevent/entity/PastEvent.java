package com.hycora.backend.domain.pastevent.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "past_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "`order`")
    private Integer order;

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

    public void update(String imageUrl, String title, String description, Integer order) {
        this.imageUrl = imageUrl;
        this.title = title;
        this.description = description;
        this.order = order;
    }

    public void updateOrder(Integer order) {
        this.order = order;
    }
}
