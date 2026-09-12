package com.bancoxyz.bff.web.auth.dto;

public record WebAuthResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        String scope
) {
}