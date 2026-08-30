package com.bancoxyz.batch.batch;

import org.springframework.batch.core.step.skip.SkipPolicy;

/**
 * Política personalizada de tolerancia a fallos.
 *
 * Solo se omiten errores de validación/conversión de datos y se permite
 * como máximo un número configurable de registros inválidos por Step.
 * Los errores de infraestructura o persistencia no se omiten.
 */
public class CustomSkipPolicy implements SkipPolicy {

    private final long maxSkips;

    public CustomSkipPolicy(long maxSkips) {
        this.maxSkips = maxSkips;
    }

    @Override
    public boolean shouldSkip(Throwable throwable, long skipCount) {
        return throwable instanceof IllegalArgumentException
                && skipCount < maxSkips;
    }
}
