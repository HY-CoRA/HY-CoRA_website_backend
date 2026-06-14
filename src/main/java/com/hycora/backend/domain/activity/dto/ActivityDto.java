package com.hycora.backend.domain.activity.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hycora.backend.domain.activity.entity.Activity;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class ActivityDto {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Getter
    @Builder
    public static class Response {
        private String id;
        private String status;
        private String statusLabel;
        private String title;
        private String desc;
        private String intro;
        private String mentor;
        private String role;
        private String place;
        private String participants;
        private String phone;
        private String recruitStart;
        private String recruitEnd;
        private String periodText;
        private List<String> schedule;
        private List<String> images;

        public static Response from(Activity a) {
            return Response.builder()
                    .id(String.valueOf(a.getId()))
                    .status(a.getStatus())
                    .statusLabel(a.getStatusLabel())
                    .title(a.getTitle())
                    .desc(a.getDesc())
                    .intro(a.getIntro())
                    .mentor(a.getMentor())
                    .role(a.getRole())
                    .place(a.getPlace())
                    .participants(a.getParticipants())
                    .phone(a.getPhone())
                    .recruitStart(a.getRecruitStart() != null ? a.getRecruitStart().toString() : null)
                    .recruitEnd(a.getRecruitEnd() != null ? a.getRecruitEnd().toString() : null)
                    .periodText(a.getPeriodText())
                    .schedule(parseList(a.getSchedule()))
                    .images(parseList(a.getImages()))
                    .build();
        }

        private static List<String> parseList(String json) {
            if (json == null) return List.of();
            try {
                return mapper.readValue(json, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                return List.of();
            }
        }
    }

    @Getter
    public static class Request {
        private String status;
        private String title;
        private String desc;
        private String intro;
        private String mentor;
        private String role;
        private String place;
        private String participants;
        private String phone;
        private String recruitStart;
        private String recruitEnd;
        private String periodText;
        private List<String> schedule;
        private List<String> images;
    }
}
