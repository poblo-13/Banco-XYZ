package com.bancoxyz.bff.atm.client;

import com.bancoxyz.bff.atm.client.dto.CoreCuentaResponse;
import com.bancoxyz.bff.atm.client.dto.CoreMovimientoRequest;
import com.bancoxyz.bff.atm.client.dto.CoreMovimientoResponse;
import com.bancoxyz.bff.atm.exception.AtmRecursoNoEncontradoException;
import com.bancoxyz.bff.atm.exception.AtmSaldoInsuficienteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class CoreApiClient {

    private static final Logger log =
            LoggerFactory.getLogger(CoreApiClient.class);

    private final RestClient coreRestClient;

    public CoreApiClient(RestClient coreRestClient) {
        this.coreRestClient = coreRestClient;
    }

    public CoreCuentaResponse obtenerCuenta(Long cuentaId) {

        log.info(
                "ATM-BFF consultando saldo de cuenta {} en Core API",
                cuentaId
        );

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
}