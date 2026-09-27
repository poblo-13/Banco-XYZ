package com.bancoxyz.bff.atm.exception;

public class AtmCoreNoDisponibleException extends RuntimeException {

    public AtmCoreNoDisponibleException(String message) {
        super(message);
    }
}