package com.bancoxyz.core.service;

import com.bancoxyz.core.entity.Cuenta;
import com.bancoxyz.core.entity.Movimiento;
import com.bancoxyz.core.exception.RecursoNoEncontradoException;
import com.bancoxyz.core.exception.SaldoInsuficienteException;
import com.bancoxyz.core.repository.CuentaRepository;
import com.bancoxyz.core.repository.MovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MovimientoService {

    private final MovimientoRepository movimientoRepository;
    private final CuentaRepository cuentaRepository;

    public MovimientoService(
            MovimientoRepository movimientoRepository,
            CuentaRepository cuentaRepository) {

        this.movimientoRepository = movimientoRepository;
        this.cuentaRepository = cuentaRepository;
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

        switch (tipo) {

            case "deposito" -> {
                cuenta.setSaldoActual(
                        cuenta.getSaldoActual().add(monto)
                );
            }

            case "retiro", "compra" -> {

                if (cuenta.getSaldoActual().compareTo(monto) < 0) {
                    throw new SaldoInsuficienteException(
                            "Saldo insuficiente para realizar la operacion"
                    );
                }

                cuenta.setSaldoActual(
                        cuenta.getSaldoActual().subtract(monto)
                );

                movimiento.setMonto(monto.negate());
            }

            default -> throw new IllegalArgumentException(
                    "Tipo de movimiento no soportado: " + tipo
            );
        }

        cuentaRepository.save(cuenta);

        return movimientoRepository.save(movimiento);
    }
}