package com.bancoxyz.bff.mobile.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record MobileAuthRequest(

        @NotBlank(message = "El usuario es obligatorio")
        String username,

        @NotBlank(message = "La contrasena es obligatoria")
        String password
) {
}