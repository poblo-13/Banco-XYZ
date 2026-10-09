package com.bancoxyz.account.controller;

import com.bancoxyz.account.entity.OperacionCuenta;
import com.bancoxyz.account.service.OperacionCuentaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/cuentas")
public class OperacionCuentaController {

    private final OperacionCuentaService operacionService;

    public OperacionCuentaController(
            OperacionCuentaService operacionService) {
        this.operacionService = operacionService;
    }

    @PostMapping("/{cuentaId}/operaciones")
    public OperacionResponse registrar(
            @PathVariable Long cuentaId,
            @Valid @RequestBody OperacionRequest request) {

        OperacionCuenta operacion = operacionService.procesar(
                request.operacionId(),
                cuentaId,
                request.tipo(),
                request.monto()
        );

        return OperacionResponse.from(operacion);
    }

    public record OperacionRequest(
            @NotBlank String operacionId,
            @NotBlank String tipo,
            @NotNull
            @DecimalMin("0.01")
            @Digits(integer = 13, fraction = 2)
            BigDecimal monto
    ) {
    }

    public record OperacionResponse(
            String operacionId,
            Long cuentaId,
            String tipo,
            BigDecimal monto,
            BigDecimal saldoResultante,
            LocalDateTime fecha
    ) {
        public static OperacionResponse from(OperacionCuenta op) {
            return new OperacionResponse(
                    op.getOperacionId(),
                    op.getCuentaId(),
                    op.getTipo(),
                    op.getMonto(),
                    op.getSaldoResultante(),
                    op.getFecha()
            );
        }
    }
}