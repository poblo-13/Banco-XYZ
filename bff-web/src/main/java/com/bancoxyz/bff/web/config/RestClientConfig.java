package com.bancoxyz.bff.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient coreRestClient(
            @Value("${account.api.base-url}") String url) {
        return crearCliente(url);
    }

    @Bean
    public RestClient paymentRestClient(
            @Value("${payment.api.base-url}") String url) {
        return crearCliente(url);
    }

    @Bean
    public RestClient customerRestClient(
            @Value("${customer.api.base-url}") String url) {
        return crearCliente(url);
    }

    private RestClient crearCliente(String url) {
        return RestClient.builder()
                .baseUrl(url)
                .requestInterceptor((request, body, execution) -> {
                    var authentication =
                            SecurityContextHolder.getContext()
                                    .getAuthentication();

                    if (!(authentication instanceof JwtAuthenticationToken jwt)) {
                        throw new IllegalStateException(
                                "No existe un token JWT autenticado");
                    }

                    request.getHeaders().setBearerAuth(
                            jwt.getToken().getTokenValue()
                    );

                    return execution.execute(request, body);
                })
                .build();
    }
}