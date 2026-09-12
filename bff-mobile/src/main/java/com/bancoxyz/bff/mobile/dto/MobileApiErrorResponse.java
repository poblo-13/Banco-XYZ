package com.bancoxyz.bff.mobile.dto;

import java.time.LocalDateTime;

public record MobileApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String path
) {
}