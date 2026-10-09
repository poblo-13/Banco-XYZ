package com.bancoxyz.bff.web.client;

import com.bancoxyz.bff.web.client.dto.CoreCuentaResponse;
import com.bancoxyz.bff.web.client.dto.CoreMovimientoResponse;
import com.bancoxyz.bff.web.exception.WebCoreNoDisponibleException;
import com.bancoxyz.bff.web.exception.WebRecursoNoEncontradoException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

@Component
public class CoreApiClient {

    private static final Logger log =
            LoggerFactory.getLogger(CoreApiClient.class);

    private final RestClient coreRestClient;
    private final RestClient customerRestClient;
    private final RestClient paymentRestClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public CoreApiClient(
            @Qualifier("coreRestClient") RestClient coreRestClient,
            @Qualifier("customerRestClient") RestClient customerRestClient,
            @Qualifier("paymentRestClient") RestClient paymentRestClient,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

        this.coreRestClient = coreRestClient;
        this.customerRestClient = customerRestClient;
        this.paymentRestClient = paymentRestClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

    public List<CoreCuentaResponse> listarCuentas() {

        return ejecutarConCircuitBreaker(() -> {

            List<DatosCuenta> cuentas = coreRestClient.get()
                    .uri("/api/cuentas")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<DatosCuenta>>() {});

            if (cuentas == null) {
                throw new IllegalStateException("Respuesta vacia de cuentas");
            }

            return cuentas.stream()
                    .map(this::completarCuenta)
                    .toList();
        });
    }


    public CoreCuentaResponse obtenerCuenta(Long cuentaId) {

        return ejecutarConCircuitBreaker(() -> {
            try {
                DatosCuenta cuenta = coreRestClient.get()
                        .uri("/api/cuentas/{cuentaId}", cuentaId)
                        .retrieve()
                        .body(DatosCuenta.class);

                if (cuenta == null) {
                    throw new IllegalStateException(
                            "Respuesta vacia de account-service");
                }

                return completarCuenta(cuenta);

            } catch (HttpClientErrorException.NotFound ex) {
                throw new WebRecursoNoEncontradoException(
                        "Cuenta o cliente no encontrado: " + cuentaId);
            }
        });
    }

    private CoreCuentaResponse completarCuenta(DatosCuenta cuenta) {

        if (cuenta.clienteId() == null) {
            throw new IllegalStateException(
                    "La cuenta no incluye clienteId");
        }

        DatosCliente cliente;

        try {
            cliente = customerRestClient.get()
                    .uri("/api/clientes/{clienteId}", cuenta.clienteId())
                    .retrieve()
                    .body(DatosCliente.class);

        } catch (HttpClientErrorException.NotFound ex) {
            throw new WebRecursoNoEncontradoException(
                    "Cliente no encontrado: " + cuenta.clienteId());
        }

        if (cliente == null) {
            throw new IllegalStateException(
                    "Respuesta vacia de customer-service");
        }

        return new CoreCuentaResponse(
                cuenta.cuentaId(),
                cliente.nombre(),
                cliente.edad(),
                cuenta.tipo(),
                cuenta.saldoActual(),
                cuenta.tasaInteres(),
                cuenta.interesCalculado()
        );
    }

    public List<CoreMovimientoResponse> obtenerMovimientos(Long cuentaId) {

        return ejecutarConCircuitBreaker(() -> {

            List<CoreMovimientoResponse> movimientos =
                    paymentRestClient.get()
                            .uri(
                                "/api/pagos/cuentas/{cuentaId}/movimientos",
                                cuentaId
                            )
                            .retrieve()
                            .body(
                                new ParameterizedTypeReference<
                                        List<CoreMovimientoResponse>>() {}
                            );

            if (movimientos == null) {
                throw new IllegalStateException(
                        "Respuesta vacia de payment-service");
            }

            return movimientos;
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

            if (actual instanceof WebRecursoNoEncontradoException ex) {
                throw ex;
            }

            actual = actual.getCause();
        }

        log.error(
                "Error de comunicacion con los microservicios",
                throwable
        );

        throw new WebCoreNoDisponibleException(
                "Los servicios bancarios no estan disponibles temporalmente"
        );
    }

    private record DatosCuenta(
            Long cuentaId,
            Long clienteId,
            String tipo,
            BigDecimal saldoActual,
            BigDecimal tasaInteres,
            BigDecimal interesCalculado
    ) {
    }

    private record DatosCliente(
            String nombre,
            Integer edad
    ) {
    }
}