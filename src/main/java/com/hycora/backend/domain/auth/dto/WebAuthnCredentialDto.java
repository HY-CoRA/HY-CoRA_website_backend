package com.hycora.backend.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WebAuthnCredentialDto {

    private String email;
    private Credential credential;

    @Getter
    @NoArgsConstructor
    public static class Credential {
        private String id;
        private String type;
        private String rawId;
        private Response response;
    }

    @Getter
    @NoArgsConstructor
    public static class Response {
        private String authenticatorData;
        private String clientDataJSON;
        private String signature;
        private String userHandle;
        private String attestationObject;
    }
}
