package com.bancoxyz.bff.atm.client;

import com.bancoxyz.bff.atm.client.dto.CoreCuentaResponse;
import com.bancoxyz.bff.atm.client.dto.CoreMovimientoRequest;
import com.bancoxyz.bff.atm.client.dto.CoreMovimientoResponse;
import com.bancoxyz.bff.atm.exception.AtmCoreNoDisponibleException;
import com.bancoxyz.bff.atm.exception.AtmRecursoNoEncontradoException;
import com.bancoxyz.bff.atm.exception.AtmSaldoInsuficienteException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.function.Supplier;

@Component
public class CoreApiClient {

    private static final Logger log =
            LoggerFactory.getLogger(CoreApiClient.class);

    private final RestClient accountRestClient;
    private final RestClient paymentRestClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public CoreApiClient(
            @Qualifier("accountRestClient") RestClient accountRestClient,
            @Qualifier("paymentRestClient") RestClient paymentRestClient,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

        this.accountRestClient = accountRestClient;
        this.paymentRestClient = paymentRestClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public CoreCuentaResponse obtenerCuenta(Long cuentaId) {

        return ejecutarConCircuitBreaker(() -> {
            try {
                return accountRestClient.get()
                        .uri("/api/cuentas/{cuentaId}", cuentaId)
                        .retrieve()
                        .body(CoreCuentaResponse.class);

            } catch (HttpClientErrorException.NotFound ex) {
                throw new AtmRecursoNoEncontradoException(
                        "Cuenta no encontrada: " + cuentaId);
            }
        });
    }

    public CoreMovimientoResponse realizarRetiro(
            String operacionId,
            Long cuentaId,
            BigDecimal monto) {

        return ejecutarConCircuitBreaker(() -> {

            CoreMovimientoRequest request =
                    new CoreMovimientoRequest(
                            operacionId,
                            cuentaId,
                            "RETIRO",
                            monto,
                            "Retiro realizado desde ATM"
                    );

            try {
                return paymentRestClient.post()
                        .uri("/api/pagos/movimientos")
                        .body(request)
                        .retrieve()
                        .body(CoreMovimientoResponse.class);

            } catch (HttpClientErrorException.NotFound ex) {
                throw new AtmRecursoNoEncontradoException(
                        "Cuenta no encontrada: " + cuentaId);

            } catch (HttpClientErrorException.Conflict ex) {
                throw new AtmSaldoInsuficienteException(
                        "Retiro rechazado: saldo insuficiente o conflicto de operacion");
            }
        });
    }

    private <T> T ejecutarConCircuitBreaker(Supplier<T> operacion) {

        Authentication autenticacion =
                SecurityContextHolder.getContext().getAuthentication();

        return circuitBreakerFactory
                .create("coreApi")
                .run(() -> {

                    SecurityContext anterior =
                            SecurityContextHolder.getContext();

                    SecurityContext propagado =
                            SecurityContextHolder.createEmptyContext();

                    propagado.setAuthentication(autenticacion);

                    try {
                        SecurityContextHolder.setContext(propagado);
                        return operacion.get();
                    } finally {
                        SecurityContextHolder.setContext(anterior);
                    }

                }, this::manejarFallo);
    }

    private <T> T manejarFallo(Throwable throwable) {

        Throwable actual = throwable;

        while (actual != null) {

            if (actual instanceof AtmRecursoNoEncontradoException ex) {
                throw ex;
            }

            if (actual instanceof AtmSaldoInsuficienteException ex) {
                throw ex;
            }

            actual = actual.getCause();
        }

        log.error("ATM: fallo de comunicacion entre microservicios", throwable);

        throw new AtmCoreNoDisponibleException(
                "Los servicios bancarios no estan disponibles temporalmente");
    }
}