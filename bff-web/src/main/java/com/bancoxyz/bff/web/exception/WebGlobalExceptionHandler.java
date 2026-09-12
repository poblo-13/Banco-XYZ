package com.bancoxyz.bff.web.exception;

import com.bancoxyz.bff.web.dto.WebApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class WebGlobalExceptionHandler {

    @ExceptionHandler(WebRecursoNoEncontradoException.class)
    public ResponseEntity<WebApiErrorResponse> manejarNoEncontrado(
            WebRecursoNoEncontradoException ex,
            HttpServletRequest request) {

        WebApiErrorResponse error =
                new WebApiErrorResponse(
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
}