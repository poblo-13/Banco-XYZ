package com.bancoxyz.batch.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryPolicyTest {

    @Test
    void debeReintentarErroresTransitoriosHastaTenerExito()
            throws RetryException {

        BatchInfrastructureConfig config =
                new BatchInfrastructureConfig();

        // Valores pequeños para que el test sea rápido
        ReflectionTestUtils.setField(
                config, "retryLimit", 3);

        ReflectionTestUtils.setField(
                config, "retryInitialInterval", 1L);

        ReflectionTestUtils.setField(
                config, "retryMultiplier", 2.0);

        ReflectionTestUtils.setField(
                config, "retryMaxInterval", 4L);

        RetryPolicy retryPolicy =
                config.batchRetryPolicy();

        RetryTemplate retryTemplate =
                new RetryTemplate(retryPolicy);

        AtomicInteger intentos =
                new AtomicInteger();

        String resultado =
                retryTemplate.execute(() -> {

                    int intento =
                            intentos.incrementAndGet();

                    if (intento <= 3) {

                        throw new
                                TransientDataAccessResourceException(
                                "Fallo transitorio simulado");
                    }

                    return "OK";
                });

        assertEquals("OK", resultado);
        assertEquals(4, intentos.get());
    }

    @Test
    void noDebeReintentarErroresNoTransitorios() {

        BatchInfrastructureConfig config =
                new BatchInfrastructureConfig();

        ReflectionTestUtils.setField(
                config, "retryLimit", 3);

        ReflectionTestUtils.setField(
                config, "retryInitialInterval", 1L);

        ReflectionTestUtils.setField(
                config, "retryMultiplier", 2.0);

        ReflectionTestUtils.setField(
                config, "retryMaxInterval", 4L);

        RetryPolicy retryPolicy =
                config.batchRetryPolicy();

        RetryTemplate retryTemplate =
                new RetryTemplate(retryPolicy);

        AtomicInteger intentos =
                new AtomicInteger();

        assertThrows(
                RetryException.class,
                () -> retryTemplate.execute(() -> {

                    intentos.incrementAndGet();

                    throw new IllegalArgumentException(
                            "Error de validación");
                }));

        assertEquals(1, intentos.get());
    }
}