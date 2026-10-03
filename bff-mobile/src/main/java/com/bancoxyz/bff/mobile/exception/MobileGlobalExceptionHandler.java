package com.bancoxyz.bff.mobile.exception;

import com.bancoxyz.bff.mobile.dto.MobileApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class MobileGlobalExceptionHandler {

    @ExceptionHandler(MobileRecursoNoEncontradoException.class)
    public ResponseEntity<MobileApiErrorResponse> manejarNoEncontrado(
            MobileRecursoNoEncontradoException ex,
            HttpServletRequest request) {

        MobileApiErrorResponse error =
                new MobileApiErrorResponse(
                        LocalDateTime.now(),
                        HttpStatus.NOT_FOUND.value(),
                        "NOT_FOUND",
                        ex.getMessage(),
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(MobileCoreNoDisponibleException.class)
    public ResponseEntity<MobileApiErrorResponse> manejarCoreNoDisponible(
            MobileCoreNoDisponibleException ex,
            HttpServletRequest request) {

        MobileApiErrorResponse error =
                new MobileApiErrorResponse(
                        LocalDateTime.now(),
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "CORE_NO_DISPONIBLE",
                        ex.getMessage(),
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error);
    }
}