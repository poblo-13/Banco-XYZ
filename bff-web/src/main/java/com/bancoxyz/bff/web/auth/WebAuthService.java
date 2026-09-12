package com.bancoxyz.bff.web.auth;

import com.bancoxyz.bff.web.auth.dto.WebAuthRequest;
import com.bancoxyz.bff.web.auth.dto.WebAuthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class WebAuthService {

    private final JwtEncoder jwtEncoder;
    private final String username;
    private final String password;
    private final String issuer;
    private final long expirationMinutes;

    public WebAuthService(
            JwtEncoder jwtEncoder,
            @Value("${security.auth.username}") String username,
            @Value("${security.auth.password}") String password,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.expiration-minutes}") long expirationMinutes) {

        this.jwtEncoder = jwtEncoder;
        this.username = username;
        this.password = password;
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    public WebAuthResponse autenticar(WebAuthRequest request) {

        if (!username.equals(request.username())
                || !password.equals(request.password())) {

            throw new BadCredentialsException(
                    "Credenciales Web incorrectas"
            );
        }

        Instant ahora = Instant.now();
        Instant expiracion =
                ahora.plus(Duration.ofMinutes(expirationMinutes));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(username)
                .issuedAt(ahora)
                .expiresAt(expiracion)
                .claim("scope", "WEB_ACCESS")
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        String token = jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                header,
                                claims
                        )
                )
                .getTokenValue();

        return new WebAuthResponse(
                "Bearer",
                token,
                Duration.ofMinutes(expirationMinutes).toSeconds(),
                "WEB_ACCESS"
        );
    }
}