package com.bancoxyz.bff.web.client;

import com.bancoxyz.bff.web.client.dto.CoreCuentaResponse;
import com.bancoxyz.bff.web.client.dto.CoreMovimientoResponse;
import com.bancoxyz.bff.web.exception.WebRecursoNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class CoreApiClient {

    private static final Logger log =
            LoggerFactory.getLogger(CoreApiClient.class);

    private final RestClient coreRestClient;

    public CoreApiClient(RestClient coreRestClient) {
        this.coreRestClient = coreRestClient;
    }

    public List<CoreCuentaResponse> listarCuentas() {

        log.info("WEB-BFF solicitando listado de cuentas al Core API");

        return coreRestClient.get()
                .uri("/api/core/cuentas")
                .retrieve()
                .body(new ParameterizedTypeReference<List<CoreCuentaResponse>>() {});
    }

    public CoreCuentaResponse obtenerCuenta(Long cuentaId) {

        log.info(
                "WEB-BFF solicitando cuenta {} al Core API",
                cuentaId
        );

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
}