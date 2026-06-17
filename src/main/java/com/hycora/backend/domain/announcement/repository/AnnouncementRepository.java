package com.hycora.backend.domain.announcement.repository;

import com.hycora.backend.domain.announcement.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    Page<Announcement> findAllByPublishedTrue(Pageable pageable);
    Page<Announcement> findAllByPublishedTrueAndCategory(String category, Pageable pageable);
    Optional<Announcement> findByIdAndPublishedTrue(Long id);
}
