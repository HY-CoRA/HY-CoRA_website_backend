package com.hycora.backend.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponseDto {
    private String token;
    private UserDto user;

    @Getter
    @Builder
    public static class UserDto {
        private String id;
        private String email;
        private String role;
    }
}
