package com.hycora.backend.domain.siteconfig.dto;

import lombok.Getter;

public class SiteConfigDto {

    @Getter
    public static class Request {
        private Object imageUrl;
        private Object altText;
        private Object newMember;
        private Object returning;
    }
}
