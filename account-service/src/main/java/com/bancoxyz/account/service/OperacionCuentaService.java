package com.bancoxyz.account.service;

import com.bancoxyz.account.entity.Cuenta;
import com.bancoxyz.account.entity.OperacionCuenta;
import com.bancoxyz.account.repository.CuentaRepository;
import com.bancoxyz.account.repository.OperacionCuentaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class OperacionCuentaService {

    private final CuentaRepository cuentaRepository;
    private final OperacionCuentaRepository operacionRepository;

    public OperacionCuentaService(
            CuentaRepository cuentaRepository,
            OperacionCuentaRepository operacionRepository) {
        this.cuentaRepository = cuentaRepository;
        this.operacionRepository = operacionRepository;
    }

    @Transactional
    public OperacionCuenta procesar(
            String operacionId, Long cuentaId,
            String tipo, BigDecimal monto) {

        try {
            UUID.fromString(operacionId);
            if (operacionId.length() != 36) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Identificador de operacion invalido");
        }

        if (cuentaId == null || cuentaId <= 0 ||
                monto == null || monto.signum() <= 0 ||
                monto.scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cuenta o monto invalido");
        }

        String tipoNormalizado = tipo == null ? "" :
                tipo.trim().toUpperCase(Locale.ROOT);

        if (!tipoNormalizado.equals("DEPOSITO") &&
                !tipoNormalizado.equals("RETIRO")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tipo de operacion no permitido");
        }

        Cuenta cuenta = cuentaRepository.bloquearPorId(cuentaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cuenta no encontrada"));

        var existente = operacionRepository.findById(operacionId);

        if (existente.isPresent()) {
            OperacionCuenta anterior = existente.get();

            if (!Objects.equals(anterior.getCuentaId(), cuentaId) ||
                    !anterior.getTipo().equals(tipoNormalizado) ||
                    anterior.getMonto().compareTo(monto) != 0 ||
                    anterior.getCuentaDestinoId() != null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Identificador utilizado para otra operacion");
            }

            return anterior;
        }

        BigDecimal nuevoSaldo;

        if (tipoNormalizado.equals("DEPOSITO")) {
            nuevoSaldo = cuenta.getSaldoActual().add(monto);
        } else {
            if (cuenta.getSaldoActual().compareTo(monto) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Saldo insuficiente");
            }

            nuevoSaldo = cuenta.getSaldoActual().subtract(monto);
        }

        cuenta.setSaldoActual(nuevoSaldo);
        cuentaRepository.save(cuenta);

        OperacionCuenta operacion = new OperacionCuenta(
                operacionId, cuentaId, null,
                tipoNormalizado, monto, nuevoSaldo);

        return operacionRepository.saveAndFlush(operacion);
    }
}