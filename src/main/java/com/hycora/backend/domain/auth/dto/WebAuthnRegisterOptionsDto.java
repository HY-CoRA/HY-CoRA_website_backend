package com.hycora.backend.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WebAuthnRegisterOptionsDto {
    private String email;
    private String displayName;
}
