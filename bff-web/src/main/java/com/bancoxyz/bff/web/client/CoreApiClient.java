package com.bancoxyz.bff.web.client;

import com.bancoxyz.bff.web.client.dto.CoreCuentaResponse;
import com.bancoxyz.bff.web.client.dto.CoreMovimientoResponse;
import com.bancoxyz.bff.web.exception.WebCoreNoDisponibleException;
import com.bancoxyz.bff.web.exception.WebRecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
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

    public List<CoreCuentaResponse> listarCuentas() {

        log.info("WEB-BFF solicitando listado de cuentas al Core API");

        return ejecutarConCircuitBreaker(
                () -> coreRestClient.get()
                        .uri("/api/core/cuentas")
                        .retrieve()
                        .body(
                                new ParameterizedTypeReference<
                                        List<CoreCuentaResponse>>() {}
                        )
        );
    }

    public CoreCuentaResponse obtenerCuenta(Long cuentaId) {

        log.info(
                "WEB-BFF solicitando cuenta {} al Core API",
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

            throw new WebRecursoNoEncontradoException(
                    "Cuenta no encontrada: " + cuentaId
            );
        }
    }

    public List<CoreMovimientoResponse> obtenerMovimientos(Long cuentaId) {

        log.info(
                "WEB-BFF solicitando movimientos de cuenta {} al Core API",
                cuentaId
        );

        return ejecutarConCircuitBreaker(
                () -> obtenerMovimientosDesdeCore(cuentaId)
        );
    }

    private List<CoreMovimientoResponse> obtenerMovimientosDesdeCore(
            Long cuentaId) {

        try {
            return coreRestClient.get()
                    .uri(
                            "/api/core/cuentas/{cuentaId}/movimientos",
                            cuentaId
                    )
                    .retrieve()
                    .body(
                            new ParameterizedTypeReference<
                                    List<CoreMovimientoResponse>>() {}
                    );

        } catch (HttpClientErrorException.NotFound ex) {

            throw new WebRecursoNoEncontradoException(
                    "Cuenta no encontrada: " + cuentaId
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

        WebRecursoNoEncontradoException noEncontrado =
                buscarCausa(
                        throwable,
                        WebRecursoNoEncontradoException.class
                );

        if (noEncontrado != null) {
            throw noEncontrado;
        }

        log.error(
                "Circuit Breaker WEB: Core API no disponible. Tipo de error: {}",
                throwable.getClass().getName(),
                throwable
        );

        throw new WebCoreNoDisponibleException(
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