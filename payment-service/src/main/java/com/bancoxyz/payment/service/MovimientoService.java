package com.bancoxyz.payment.service;

import com.bancoxyz.payment.client.CuentaClient;
import com.bancoxyz.payment.entity.Movimiento;
import com.bancoxyz.payment.repository.MovimientoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class MovimientoService {

    private final MovimientoRepository repository;
    private final CuentaClient cuentaClient;
    private final TransactionTemplate transaccion;

    public MovimientoService(
            MovimientoRepository repository,
            CuentaClient cuentaClient,
            PlatformTransactionManager transactionManager) {

        this.repository = repository;
        this.cuentaClient = cuentaClient;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    public Movimiento procesar(
            String operacionId,
            Long cuentaId,
            String tipo,
            BigDecimal monto,
            String descripcion) {

        validarIdentificador(operacionId);

        String tipoNormalizado = tipo == null ? "" :
                tipo.trim().toUpperCase(Locale.ROOT);

        if (!tipoNormalizado.equals("DEPOSITO") &&
                !tipoNormalizado.equals("RETIRO")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tipo de movimiento invalido");
        }

        if (cuentaId == null || cuentaId <= 0 ||
                monto == null || monto.signum() <= 0 ||
                monto.scale() > 2 ||
                monto.precision() - monto.scale() > 13) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cuenta o monto invalido");
        }

        if (descripcion != null && descripcion.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Descripcion demasiado larga");
        }

        Movimiento movimiento;

        try {
            movimiento = transaccion.execute(status ->
                    repository.findByOperacionId(operacionId)
                            .orElseGet(() -> repository.saveAndFlush(
                                    new Movimiento(
                                            operacionId,
                                            cuentaId,
                                            tipoNormalizado,
                                            monto,
                                            descripcion
                                    )
                            ))
            );
        } catch (DataIntegrityViolationException ex) {
            // Otra solicitud pudo registrar el mismo ID primero.
            movimiento = repository.findByOperacionId(operacionId)
                    .orElseThrow(() -> ex);
        }

        if (!Objects.equals(movimiento.getCuentaId(), cuentaId) ||
                !movimiento.getTipoMovimiento().equals(tipoNormalizado) ||
                movimiento.getMonto().compareTo(monto) != 0 ||
                !Objects.equals(movimiento.getDescripcion(), descripcion)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Identificador utilizado para otra operacion");
        }

        if (movimiento.getEstado() == Movimiento.Estado.CONFIRMADO) {
            return movimiento;
        }

        if (movimiento.getEstado() == Movimiento.Estado.RECHAZADO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La operacion ya fue rechazada");
        }

        CuentaClient.OperacionResponse resultado;

        try {
            resultado = cuentaClient.procesarOperacion(
                    operacionId, cuentaId, tipoNormalizado, monto
            );
        } catch (RestClientResponseException ex) {

            int codigo = ex.getStatusCode().value();

            if (codigo == 400 || codigo == 404 || codigo == 409) {
                transaccion.execute(status -> {
                    Movimiento actual = repository
                            .findByOperacionId(operacionId)
                            .orElseThrow();

                    if (actual.getEstado() == Movimiento.Estado.PENDIENTE) {
                        actual.rechazar();
                        repository.save(actual);
                    }

                    return null;
                });

                throw new ResponseStatusException(
                        ex.getStatusCode(),
                        "Operacion rechazada por account-service");
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Operacion pendiente: no se pudo confirmar el resultado",
                    ex
            );

        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Operacion pendiente: error de comunicacion",
                    ex
            );
        }

        if (resultado == null ||
                !operacionId.equals(resultado.operacionId()) ||
                !Objects.equals(cuentaId, resultado.cuentaId()) ||
                !tipoNormalizado.equals(resultado.tipo()) ||
                resultado.monto() == null ||
                resultado.monto().compareTo(monto) != 0 ||
                resultado.saldoResultante() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Respuesta inconsistente de account-service");
        }

        return transaccion.execute(status -> {
            Movimiento actual = repository
                    .findByOperacionId(operacionId)
                    .orElseThrow();

            if (actual.getEstado() == Movimiento.Estado.PENDIENTE) {
                actual.confirmar(resultado.saldoResultante());
                repository.save(actual);
            }

            return actual;
        });
    }

    private void validarIdentificador(String operacionId) {
        try {
            if (operacionId == null ||
                    !UUID.fromString(operacionId)
                            .toString().equals(operacionId)) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Identificador de operacion invalido");
        }
    }
}