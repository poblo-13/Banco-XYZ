package com.bancoxyz.bff.atm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient accountRestClient(
            @Value("${account.api.base-url}") String baseUrl) {

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor((request, body, execution) -> {

                    var auth = SecurityContextHolder
                            .getContext()
                            .getAuthentication();

                    if (!(auth instanceof JwtAuthenticationToken jwt)) {
                        throw new IllegalStateException(
                                "No existe autenticacion JWT del ATM");
                    }

                    request.getHeaders().setBearerAuth(
                            jwt.getToken().getTokenValue()
                    );

                    return execution.execute(request, body);
                })
                .build();
    }

    @Bean
    public RestClient paymentRestClient(
            @Value("${payment.api.base-url}") String baseUrl,
            @Value("${atm.payment.token-url}") String tokenUrl,
            @Value("${atm.payment.client-id}") String clientId,
            @Value("${atm.payment.client-secret}") String clientSecret) {

        RestClient tokenClient = RestClient.create();

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor((request, body, execution) -> {

                    var formulario = new LinkedMultiValueMap<String, String>();
                    formulario.add("grant_type", "client_credentials");
                    formulario.add("scope", "PAGOS_WRITE");

                    Map<?, ?> respuesta = tokenClient.post()
                            .uri(tokenUrl)
                            .headers(h -> h.setBasicAuth(clientId, clientSecret))
                            .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                            .body(formulario)
                            .retrieve()
                            .body(Map.class);

                    if (respuesta == null ||
                            !(respuesta.get("access_token") instanceof String token) ||
                            token.isBlank()) {
                        throw new IllegalStateException(
                                "No se obtuvo token para operaciones ATM");
                    }

                    request.getHeaders().setBearerAuth(token);
                    return execution.execute(request, body);
                })
                .build();
    }
}