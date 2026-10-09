package com.bancoxyz.customer.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )
            .authorizeHttpRequests(auth -> auth

                .dispatcherTypeMatchers(DispatcherType.ERROR)
                .permitAll()

                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**"
                ).permitAll()

                .requestMatchers(HttpMethod.GET, "/api/clientes")
                .hasAuthority("SCOPE_WEB_ACCESS")

                .requestMatchers(HttpMethod.GET, "/api/clientes/{id}")
                .hasAnyAuthority(
                    "SCOPE_WEB_ACCESS",
                    "SCOPE_MOBILE_ACCESS"
                )

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/clientes"
                ).hasAuthority("SCOPE_CLIENTES_WRITE")

                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/clientes/{id}"
                ).hasAuthority("SCOPE_CLIENTES_WRITE")

                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(Customizer.withDefaults())
            );

        return http.build();
    }
}