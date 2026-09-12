package com.bancoxyz.bff.web.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record WebAuthRequest(

        @NotBlank(message = "El usuario es obligatorio")
        String username,

        @NotBlank(message = "La contrasena es obligatoria")
        String password
) {
}