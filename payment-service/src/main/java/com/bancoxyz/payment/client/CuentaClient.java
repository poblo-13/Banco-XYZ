package com.bancoxyz.payment.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Component
public class CuentaClient {

    private final RestClient restClient = RestClient.create();

    private final String accountUrl;
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;

    public CuentaClient(
            @Value("${banco.xyz.pagos.account-url}") String accountUrl,
            @Value("${banco.xyz.pagos.token-url}") String tokenUrl,
            @Value("${banco.xyz.pagos.oauth-client-id}") String clientId,
            @Value("${banco.xyz.pagos.oauth-client-secret}") String clientSecret) {

        this.accountUrl = accountUrl.replaceAll("/+$", "");
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    private String obtenerToken() {

        MultiValueMap<String, String> formulario =
                new LinkedMultiValueMap<>();

        formulario.add("grant_type", "client_credentials");
        formulario.add("scope", "CUENTAS_OPERAR");

        Map<?, ?> respuesta = restClient.post()
                .uri(tokenUrl)
                .headers(headers ->
                        headers.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formulario)
                .retrieve()
                .body(Map.class);

        if (respuesta == null ||
                !(respuesta.get("access_token") instanceof String token) ||
                token.isBlank()) {

            throw new IllegalStateException(
                    "Authorization Server no entrego un token valido");
        }

        return token;
    }

    public OperacionResponse procesarOperacion(
            String operacionId,
            Long cuentaId,
            String tipo,
            BigDecimal monto) {

        String token = obtenerToken();

        OperacionRequest solicitud =
                new OperacionRequest(operacionId, tipo, monto);

        return restClient.post()
                .uri(
                        accountUrl + "/api/cuentas/{cuentaId}/operaciones",
                        cuentaId
                )
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(solicitud)
                .retrieve()
                .body(OperacionResponse.class);
    }

    public record OperacionRequest(
            String operacionId,
            String tipo,
            BigDecimal monto
    ) {
    }

    public record OperacionResponse(
            String operacionId,
            Long cuentaId,
            String tipo,
            BigDecimal monto,
            BigDecimal saldoResultante,
            LocalDateTime fecha
    ) {
    }
}