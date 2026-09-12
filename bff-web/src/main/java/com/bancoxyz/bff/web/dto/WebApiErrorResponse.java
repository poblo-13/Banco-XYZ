package com.bancoxyz.bff.web.dto;

import java.time.LocalDateTime;

public record WebApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String path
) {
}