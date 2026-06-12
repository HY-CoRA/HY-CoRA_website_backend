package com.hycora.backend.domain.pastevent.dto;

import com.hycora.backend.domain.pastevent.entity.PastEvent;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class PastEventDto {

    @Getter
    @Builder
    public static class Response {
        private String id;
        private String imageUrl;
        private String title;
        private String description;
        private Integer order;

        public static Response from(PastEvent e) {
            return Response.builder()
                    .id(String.valueOf(e.getId()))
                    .imageUrl(e.getImageUrl())
                    .title(e.getTitle())
                    .description(e.getDescription())
                    .order(e.getOrder())
                    .build();
        }
    }

    @Getter
    public static class Request {
        private String imageUrl;
        private String title;
        private String description;
        private Integer order;
    }

    @Getter
    public static class ReorderRequest {
        private List<String> ids;
    }
}
