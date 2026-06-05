package com.hycora.backend.domain.announcement.repository;

import com.hycora.backend.domain.announcement.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findAllByPublishedTrue();
    List<Announcement> findAllByCategory(String category);
}
