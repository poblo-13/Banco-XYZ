package com.bancoxyz.batch.batch;

import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

/**
 * Registra los elementos omitidos por la política de tolerancia a fallos.
 */
@Component
public class BatchSkipListener implements SkipListener<Object, Object> {

    @Override
    public void onSkipInRead(Throwable throwable) {
        System.err.println("[SKIP][READ] " + throwable.getMessage());
    }

    @Override
    public void onSkipInWrite(Object item, Throwable throwable) {
        System.err.println("[SKIP][WRITE] " + throwable.getMessage()
                + " | item=" + item);
    }

    @Override
    public void onSkipInProcess(Object item, Throwable throwable) {
        System.err.println("[SKIP][PROCESS] " + throwable.getMessage()
                + " | item=" + item);
    }
}
