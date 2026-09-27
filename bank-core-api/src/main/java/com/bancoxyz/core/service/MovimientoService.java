package com.bancoxyz.core.service;

import com.bancoxyz.core.entity.Cuenta;
import com.bancoxyz.core.entity.Movimiento;
import com.bancoxyz.core.event.MovimientoEvento;
import com.bancoxyz.core.exception.RecursoNoEncontradoException;
import com.bancoxyz.core.exception.SaldoInsuficienteException;
import com.bancoxyz.core.repository.CuentaRepository;
import com.bancoxyz.core.repository.MovimientoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovimientoService {

    private final MovimientoRepository movimientoRepository;
    private final CuentaRepository cuentaRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MovimientoService(
            MovimientoRepository movimientoRepository,
            CuentaRepository cuentaRepository,
            ApplicationEventPublisher eventPublisher) {

        this.movimientoRepository = movimientoRepository;
        this.cuentaRepository = cuentaRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<Movimiento> obtenerPorCuenta(Long cuentaId) {
        return movimientoRepository
                .findByCuentaIdOrderByFechaDescIdDesc(cuentaId);
    }

    @Transactional
    public Movimiento registrarOperacion(Movimiento movimiento) {

        Cuenta cuenta = cuentaRepository.findById(movimiento.getCuentaId())
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Cuenta no encontrada: " + movimiento.getCuentaId()
                        )
                );

        String tipo = movimiento.getTipoMovimiento()
                .trim()
                .toLowerCase();

        BigDecimal monto = movimiento.getMonto();

        String tipoEvento;

        switch (tipo) {

            case "deposito" -> {
                cuenta.setSaldoActual(
                        cuenta.getSaldoActual().add(monto)
                );

                tipoEvento = "DEPOSITO_REALIZADO";
            }

            case "retiro" -> {

                if (cuenta.getSaldoActual().compareTo(monto) < 0) {
                    throw new SaldoInsuficienteException(
                            "Saldo insuficiente para realizar la operacion"
                    );
                }

                cuenta.setSaldoActual(
                        cuenta.getSaldoActual().subtract(monto)
                );

                movimiento.setMonto(monto.negate());

                tipoEvento = "RETIRO_REALIZADO";
            }

            case "compra" -> {

                if (cuenta.getSaldoActual().compareTo(monto) < 0) {
                    throw new SaldoInsuficienteException(
                            "Saldo insuficiente para realizar la operacion"
                    );
                }

                cuenta.setSaldoActual(
                        cuenta.getSaldoActual().subtract(monto)
                );

                movimiento.setMonto(monto.negate());

                tipoEvento = "COMPRA_REALIZADA";
            }

            default -> throw new IllegalArgumentException(
                    "Tipo de movimiento no soportado: " + tipo
            );
        }

        cuentaRepository.save(cuenta);

        Movimiento guardado =
                movimientoRepository.save(movimiento);

        MovimientoEvento evento =
                new MovimientoEvento(
                        tipoEvento,
                        guardado.getId(),
                        guardado.getCuentaId(),
                        guardado.getTipoMovimiento(),
                        guardado.getMonto(),
                        cuenta.getSaldoActual(),
                        guardado.getFecha(),
                        LocalDateTime.now()
                );

        eventPublisher.publishEvent(evento);

        return guardado;
    }
}