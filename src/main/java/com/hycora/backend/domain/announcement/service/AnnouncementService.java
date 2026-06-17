package com.hycora.backend.domain.announcement.service;

import com.hycora.backend.domain.announcement.dto.AnnouncementDto;
import com.hycora.backend.domain.announcement.entity.Announcement;
import com.hycora.backend.domain.announcement.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    // 공개 목록 (published=true, ?category 필터, 페이지네이션)
    public List<AnnouncementDto.Response> getPublicList(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (category != null && !category.isBlank()) {
            return announcementRepository.findAllByPublishedTrueAndCategory(category, pageable)
                    .getContent().stream().map(AnnouncementDto.Response::from).toList();
        }
        return announcementRepository.findAllByPublishedTrue(pageable)
                .getContent().stream().map(AnnouncementDto.Response::from).toList();
    }

    // 공개 상세 (published=true만)
    public AnnouncementDto.Response getPublicOne(Long id) {
        Announcement announcement = announcementRepository.findByIdAndPublishedTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다."));
        return AnnouncementDto.Response.from(announcement);
    }

    // 어드민 전체 목록
    public List<AnnouncementDto.Response> getAdminList() {
        return announcementRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(AnnouncementDto.Response::from).toList();
    }

    // 어드민 상세
    public AnnouncementDto.Response getAdminOne(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다."));
        return AnnouncementDto.Response.from(announcement);
    }

    // 생성
    @Transactional
    public Long create(AnnouncementDto.Request req) {
        validateCategory(req.getCategory());
        Announcement announcement = Announcement.create(
                req.getCategory(), req.getTitle(), req.getSummary(), req.getContent(),
                req.getDate(), req.getPublished(), req.getSource(),
                req.getCapacity(), req.getLink()
        );
        return announcementRepository.save(announcement).getId();
    }

    // 수정
    @Transactional
    public Long update(Long id, AnnouncementDto.Request req) {
        validateCategory(req.getCategory());
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다."));
        announcement.update(
                req.getCategory(), req.getTitle(), req.getSummary(), req.getContent(),
                req.getDate(), req.getPublished(), req.getSource(),
                req.getCapacity(), req.getLink()
        );
        return announcement.getId();
    }

    // 삭제
    @Transactional
    public void delete(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다."));
        announcementRepository.delete(announcement);
    }

    // 발행 상태 토글
    @Transactional
    public boolean togglePublish(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다."));
        announcement.togglePublish();
        return Boolean.TRUE.equals(announcement.getPublished());
    }

    private void validateCategory(String category) {
        if (category == null || !java.util.Set.of("event", "recruitment", "etc").contains(category)) {
            throw new IllegalArgumentException("category는 event, recruitment, etc 중 하나여야 합니다.");
        }
    }
}
