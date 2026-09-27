package com.bancoxyz.core.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MovimientoEvento(
        String tipoEvento,
        Long movimientoId,
        Long cuentaId,
        String tipoMovimiento,
        BigDecimal monto,
        BigDecimal saldoResultante,
        LocalDate fechaMovimiento,
        LocalDateTime fechaEvento
) {
}