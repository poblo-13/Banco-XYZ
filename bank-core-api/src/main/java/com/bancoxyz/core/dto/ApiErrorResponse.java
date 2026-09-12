package com.bancoxyz.core.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String mensaje,
        String path,
        List<CampoError> detalles
) {

    public record CampoError(
            String campo,
            String mensaje
    ) {
    }
}