package com.hycora.backend.domain.announcement.dto;

import com.hycora.backend.domain.announcement.entity.Announcement;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AnnouncementDto {

    @Getter
    @NoArgsConstructor
    public static class Request {
        private String title;
        private String category;
        private String category_ko;
        private String date;
        private String summary;
        private String content;
        private String capacity;
        private String link;
        private Boolean published;
        private String lastModified;
        private String source;
    }

    @Getter
    public static class Response {
        private final String id;
        private final String title;
        private final String category;
        private final String category_ko;
        private final String date;
        private final String summary;
        private final String content;
        private final String capacity;
        private final String link;
        private final Boolean published;
        private final String lastModified;
        private final String source;

        private Response(Announcement a) {
            this.id = String.valueOf(a.getId());
            this.title = a.getTitle();
            this.category = a.getCategory();
            this.category_ko = a.getCategoryKo();
            this.date = a.getDate();
            this.summary = a.getSummary();
            this.content = a.getContent();
            this.capacity = a.getCapacity();
            this.link = a.getLink();
            this.published = a.getPublished();
            this.lastModified = a.getLastModified();
            this.source = a.getSource();
        }

        public static Response from(Announcement a) {
            return new Response(a);
        }
    }
}
