package com.hycora.backend.domain.leader.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class LeaderImageDto {

    @Getter
    @AllArgsConstructor
    public static class Response {
        private String imageUrl;
    }
}
