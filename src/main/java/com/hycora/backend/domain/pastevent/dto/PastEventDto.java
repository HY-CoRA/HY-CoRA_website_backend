package com.hycora.backend.domain.pastevent.dto;

import com.hycora.backend.domain.pastevent.entity.PastEvent;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class PastEventDto {

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String imageUrl;
        private String title;
        private String description;

        public static Response from(PastEvent e) {
            return Response.builder()
                    .id(e.getId())
                    .imageUrl(e.getImageUrl())
                    .title(e.getTitle())
                    .description(e.getDescription())
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
