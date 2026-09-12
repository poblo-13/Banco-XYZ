package com.bancoxyz.bff.atm.exception;

import com.bancoxyz.bff.atm.dto.AtmApiErrorResponse;
import com.bancoxyz.bff.atm.dto.AtmApiErrorResponse.CampoError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class AtmGlobalExceptionHandler {

    @ExceptionHandler(AtmRecursoNoEncontradoException.class)
    public ResponseEntity<AtmApiErrorResponse> manejarNoEncontrado(
            AtmRecursoNoEncontradoException ex,
            HttpServletRequest request) {

        AtmApiErrorResponse error =
                new AtmApiErrorResponse(
                        LocalDateTime.now(),
                        HttpStatus.NOT_FOUND.value(),
                        "NOT_FOUND",
                        ex.getMessage(),
                        request.getRequestURI(),
                        List.of()
                );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(AtmSaldoInsuficienteException.class)
    public ResponseEntity<AtmApiErrorResponse> manejarSaldoInsuficiente(
            AtmSaldoInsuficienteException ex,
            HttpServletRequest request) {

        AtmApiErrorResponse error =
                new AtmApiErrorResponse(
                        LocalDateTime.now(),
                        HttpStatus.CONFLICT.value(),
                        "SALDO_INSUFICIENTE",
                        ex.getMessage(),
                        request.getRequestURI(),
                        List.of()
                );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AtmApiErrorResponse> manejarValidacion(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<CampoError> detalles = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new CampoError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        AtmApiErrorResponse error =
                new AtmApiErrorResponse(
                        LocalDateTime.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        "VALIDATION_ERROR",
                        "La solicitud contiene datos invalidos",
                        request.getRequestURI(),
                        detalles
                );

        return ResponseEntity
                .badRequest()
                .body(error);
    }
}