package com.bancoxyz.bff.atm.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AtmApiErrorResponse(
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