package com.bancoxyz.bff.mobile.auth.dto;

public record MobileAuthResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        String scope
) {
}