package com.hycora.backend.domain.activity.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hycora.backend.domain.activity.dto.ActivityDto;
import com.hycora.backend.domain.activity.entity.Activity;
import com.hycora.backend.domain.activity.entity.StatusLabel;
import com.hycora.backend.domain.activity.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ObjectMapper objectMapper;

    public List<ActivityDto.Response> getAll(String status) {
        List<Activity> activities = (status != null)
                ? activityRepository.findAllByStatus(status)
                : activityRepository.findAll();
        return activities.stream().map(ActivityDto.Response::from).toList();
    }

    public ActivityDto.Response getOne(Long id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found"));
        return ActivityDto.Response.from(activity);
    }

    @Transactional
    public Long create(ActivityDto.Request req) {
        validate(req);
        Activity activity = Activity.builder()
                .status(req.getStatus())
                .statusLabel(StatusLabel.from(req.getStatus()))
                .title(req.getTitle())
                .desc(req.getDesc())
                .intro(req.getIntro())
                .mentor(req.getMentor())
                .role(req.getRole())
                .place(req.getPlace())
                .participants(req.getParticipants())
                .recruitStart(parseDate(req.getRecruitStart()))
                .recruitEnd(parseDate(req.getRecruitEnd()))
                .periodText(req.getPeriodText())
                .schedule(toJson(req.getSchedule()))
                .images(toJson(req.getImages()))
                .build();
        return activityRepository.save(activity).getId();
    }

    @Transactional
    public Long update(Long id, ActivityDto.Request req) {
        validate(req);
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found"));
        activity.update(
                req.getStatus(), req.getTitle(), req.getDesc(), req.getIntro(),
                req.getMentor(), req.getRole(), req.getPlace(), req.getParticipants(),
                parseDate(req.getRecruitStart()), parseDate(req.getRecruitEnd()),
                req.getPeriodText(), toJson(req.getSchedule()), toJson(req.getImages())
        );
        return activity.getId();
    }

    @Transactional
    public void delete(Long id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found"));
        activityRepository.delete(activity);
    }

    private void validate(ActivityDto.Request req) {
        if (req.getStatus() == null || req.getStatus().isBlank()) {
            throw new IllegalArgumentException("status는 필수입니다.");
        }
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new IllegalArgumentException("title은 필수입니다.");
        }
        // recruiting 상태일 경우 모집 기간 필수
        if ("recruiting".equals(req.getStatus())) {
            if (req.getRecruitStart() == null || req.getRecruitEnd() == null) {
                throw new IllegalArgumentException("recruiting 상태는 recruitStart, recruitEnd가 필수입니다.");
            }
        }
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        return LocalDate.parse(date);
    }

    private String toJson(List<String> list) {
        if (list == null) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 직렬화 실패");
        }
    }
}
