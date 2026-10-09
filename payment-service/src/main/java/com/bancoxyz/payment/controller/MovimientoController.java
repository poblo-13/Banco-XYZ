package com.bancoxyz.payment.controller;

import com.bancoxyz.payment.entity.Movimiento;
import com.bancoxyz.payment.service.MovimientoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/pagos")
public class MovimientoController {

    private final MovimientoService movimientoService;

    public MovimientoController(MovimientoService movimientoService) {
        this.movimientoService = movimientoService;
    }

    @PostMapping("/movimientos")
    public MovimientoResponse registrar(
            @Valid @RequestBody MovimientoRequest request) {

        Movimiento movimiento = movimientoService.procesar(
                request.operacionId(),
                request.cuentaId(),
                request.tipo(),
                request.monto(),
                request.descripcion()
        );

        return MovimientoResponse.from(movimiento);
    }

    public record MovimientoRequest(
            @NotBlank String operacionId,

            @NotNull
            @Positive
            Long cuentaId,

            @NotBlank
            String tipo,

            @NotNull
            @DecimalMin("0.01")
            @Digits(integer = 13, fraction = 2)
            BigDecimal monto,

            @Size(max = 255)
            String descripcion
    ) {
    }

    public record MovimientoResponse(
            Long id,
            String operacionId,
            Long cuentaId,
            LocalDate fecha,
            String tipoMovimiento,
            BigDecimal monto,
            String descripcion,
            Movimiento.Estado estado,
            BigDecimal saldoResultante
    ) {
        public static MovimientoResponse from(Movimiento movimiento) {
            return new MovimientoResponse(
                    movimiento.getId(),
                    movimiento.getOperacionId(),
                    movimiento.getCuentaId(),
                    movimiento.getFecha(),
                    movimiento.getTipoMovimiento(),
                    movimiento.getMonto(),
                    movimiento.getDescripcion(),
                    movimiento.getEstado(),
                    movimiento.getSaldoResultante()
            );
        }
    }
}