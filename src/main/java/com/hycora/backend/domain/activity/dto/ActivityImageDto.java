package com.hycora.backend.domain.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

public class ActivityImageDto {

    @Getter
    @AllArgsConstructor
    public static class Response {
        private List<Image> images;
    }

    @Getter
    @AllArgsConstructor
    public static class Image {
        private String imageId;
        private String imageUrl;
    }
}
