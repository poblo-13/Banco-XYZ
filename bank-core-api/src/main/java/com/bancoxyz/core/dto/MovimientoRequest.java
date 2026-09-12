package com.bancoxyz.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record MovimientoRequest(

        @NotNull(message = "La cuenta es obligatoria")
        Long cuentaId,

        @NotBlank(message = "El tipo de movimiento es obligatorio")
        String tipoMovimiento,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor que cero")
        BigDecimal monto,

        @Size(max = 255, message = "La descripcion no puede superar 255 caracteres")
        String descripcion
) {
}