package com.bancoxyz.bff.atm.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record AtmRetiroRequest(
        @NotNull UUID operacionId,

        @NotNull
        @Positive
        Long cuentaId,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal monto
) {
}