package com.bancoxyz.account.config;

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

                        .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR)
                        .permitAll()

                        .requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/cuentas")
                        .hasAuthority("SCOPE_WEB_ACCESS")

                        .requestMatchers(HttpMethod.GET, "/api/cuentas/{cuentaId}")
                        .hasAnyAuthority(
                                "SCOPE_WEB_ACCESS",
                                "SCOPE_MOBILE_ACCESS",
                                "SCOPE_ATM_ACCESS"
                        )

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/cuentas/{cuentaId}/operaciones"
                        )
                        .hasAuthority("SCOPE_CUENTAS_OPERAR")

                        .anyRequest()
                        .denyAll()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(Customizer.withDefaults())
                );

        return http.build();
    }
}