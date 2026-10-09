package com.bancoxyz.bff.atm.client.dto;

import java.math.BigDecimal;

public record CoreMovimientoRequest(
        String operacionId,
        Long cuentaId,
        String tipo,
        BigDecimal monto,
        String descripcion
) {
}