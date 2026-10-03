package com.bancoxyz.bff.web.exception;

public class WebCoreNoDisponibleException extends RuntimeException {

    public WebCoreNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}