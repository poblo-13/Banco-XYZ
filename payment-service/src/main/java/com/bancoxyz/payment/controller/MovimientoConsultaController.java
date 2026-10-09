package com.bancoxyz.payment.controller;

import com.bancoxyz.payment.entity.Movimiento;
import com.bancoxyz.payment.repository.MovimientoRepository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class MovimientoConsultaController {

    private final MovimientoRepository repository;

    public MovimientoConsultaController(MovimientoRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/cuentas/{cuentaId}/movimientos")
    public List<MovimientoResumenResponse> obtenerMovimientos(
            @PathVariable Long cuentaId) {

        return repository
                .findByCuentaIdAndEstadoOrderByFechaDescIdDesc(
                        cuentaId,
                        Movimiento.Estado.CONFIRMADO
                )
                .stream()
                .map(MovimientoResumenResponse::from)
                .toList();
    }

    public record MovimientoResumenResponse(
            Long id,
            Long cuentaId,
            LocalDate fecha,
            String tipoMovimiento,
            BigDecimal monto,
            String descripcion
    ) {
        public static MovimientoResumenResponse from(Movimiento movimiento) {
            return new MovimientoResumenResponse(
                    movimiento.getId(),
                    movimiento.getCuentaId(),
                    movimiento.getFecha(),
                    movimiento.getTipoMovimiento(),
                    movimiento.getMonto(),
                    movimiento.getDescripcion()
            );
        }
    }
}