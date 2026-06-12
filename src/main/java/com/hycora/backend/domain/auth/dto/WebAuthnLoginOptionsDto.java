package com.hycora.backend.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WebAuthnLoginOptionsDto {
    private String email;
}
