package com.bancoxyz.bff.atm.auth.dto;

public record AtmAuthResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        String scope
) {
}