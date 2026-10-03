package com.bancoxyz.bff.atm.client;

import com.bancoxyz.bff.atm.client.dto.CoreCuentaResponse;
import com.bancoxyz.bff.atm.client.dto.CoreMovimientoRequest;
import com.bancoxyz.bff.atm.client.dto.CoreMovimientoResponse;
import com.bancoxyz.bff.atm.exception.AtmCoreNoDisponibleException;
import com.bancoxyz.bff.atm.exception.AtmRecursoNoEncontradoException;
import com.bancoxyz.bff.atm.exception.AtmSaldoInsuficienteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.function.Supplier;

@Component
public class CoreApiClient {

    private static final Logger log =
            LoggerFactory.getLogger(CoreApiClient.class);

    private final RestClient coreRestClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public CoreApiClient(
            RestClient coreRestClient,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

        this.coreRestClient = coreRestClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public CoreCuentaResponse obtenerCuenta(Long cuentaId) {

        log.info(
                "ATM-BFF consultando saldo de cuenta {} en Core API",
                cuentaId
        );

        return ejecutarConCircuitBreaker(
                () -> obtenerCuentaDesdeCore(cuentaId)
        );
    }

    private CoreCuentaResponse obtenerCuentaDesdeCore(Long cuentaId) {

        try {
            return coreRestClient.get()
                    .uri("/api/core/cuentas/{cuentaId}", cuentaId)
                    .retrieve()
                    .body(CoreCuentaResponse.class);

        } catch (HttpClientErrorException.NotFound ex) {

            throw new AtmRecursoNoEncontradoException(
                    "Cuenta no encontrada: " + cuentaId
            );
        }
    }

    public CoreMovimientoResponse realizarRetiro(
            Long cuentaId,
            BigDecimal monto) {

        log.info(
                "ATM-BFF solicitando retiro para cuenta {}",
                cuentaId
        );

        return ejecutarConCircuitBreaker(
                () -> realizarRetiroDesdeCore(cuentaId, monto)
        );
    }

    private CoreMovimientoResponse realizarRetiroDesdeCore(
            Long cuentaId,
            BigDecimal monto) {

        CoreMovimientoRequest request =
                new CoreMovimientoRequest(
                        cuentaId,
                        "retiro",
                        monto,
                        "Retiro realizado desde ATM"
                );

        try {
            return coreRestClient.post()
                    .uri("/api/core/movimientos")
                    .body(request)
                    .retrieve()
                    .body(CoreMovimientoResponse.class);

        } catch (HttpClientErrorException.NotFound ex) {

            throw new AtmRecursoNoEncontradoException(
                    "Cuenta no encontrada: " + cuentaId
            );

        } catch (HttpClientErrorException.Conflict ex) {

            throw new AtmSaldoInsuficienteException(
                    "Saldo insuficiente para realizar el retiro"
            );
        }
    }

    private <T> T ejecutarConCircuitBreaker(Supplier<T> operacion) {

        return circuitBreakerFactory
                .create("coreApi")
                .run(
                        operacion,
                        this::manejarFalloCore
                );
    }

    private <T> T manejarFalloCore(Throwable throwable) {

        AtmRecursoNoEncontradoException noEncontrado =
                buscarCausa(
                        throwable,
                        AtmRecursoNoEncontradoException.class
                );

        if (noEncontrado != null) {
            throw noEncontrado;
        }

        AtmSaldoInsuficienteException saldoInsuficiente =
                buscarCausa(
                        throwable,
                        AtmSaldoInsuficienteException.class
                );

        if (saldoInsuficiente != null) {
            throw saldoInsuficiente;
        }

        log.error(
                "Circuit Breaker ATM: Core API no disponible. Tipo de error: {}",
                throwable.getClass().getName(),
                throwable
        );

        throw new AtmCoreNoDisponibleException(
                "El servicio central del banco no se encuentra disponible temporalmente"
        );
    }

    private <T extends Throwable> T buscarCausa(
            Throwable throwable,
            Class<T> tipo) {

        Throwable actual = throwable;

        while (actual != null) {

            if (tipo.isInstance(actual)) {
                return tipo.cast(actual);
            }

            actual = actual.getCause();
        }

        return null;
    }
}