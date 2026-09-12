package com.bancoxyz.bff.atm.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AtmRetiroRequest(

        @NotNull(message = "La cuenta es obligatoria")
        @Positive(message = "La cuenta debe ser valida")
        Long cuentaId,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor que cero")
        BigDecimal monto
) {
}