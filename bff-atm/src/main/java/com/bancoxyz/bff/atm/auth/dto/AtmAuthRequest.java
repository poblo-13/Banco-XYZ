package com.bancoxyz.bff.atm.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AtmAuthRequest(

        @NotBlank(message = "El usuario es obligatorio")
        String username,

        @NotBlank(message = "La contrasena es obligatoria")
        String password
) {
}