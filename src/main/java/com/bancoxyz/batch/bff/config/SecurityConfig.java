package com.bancoxyz.batch.bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/web/**").hasRole("WEB")
                        .requestMatchers("/api/mobile/**").hasRole("MOBILE")
                        .requestMatchers("/api/atm/**").hasRole("ATM")
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("web")
                        .password(encoder.encode("web123"))
                        .roles("WEB")
                        .build(),

                User.withUsername("mobile")
                        .password(encoder.encode("mobile123"))
                        .roles("MOBILE")
                        .build(),

                User.withUsername("atm")
                        .password(encoder.encode("atm123"))
                        .roles("ATM")
                        .build()
        );
    }
}