package com.bancoxyz.batch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * Infraestructura transversal de los Jobs.
 *
 * Se limita la ejecución a exactamente 3 tareas concurrentes para cumplir
 * la política de escalamiento solicitada para la actividad.
 */
@Configuration
public class BatchInfrastructureConfig {

    public static final int NUMERO_HILOS = 3;
    public static final int CHUNK_SIZE = 5;
    public static final int MAX_SKIPS = 10;

    @Bean
    public SimpleAsyncTaskExecutor batchTaskExecutor() {
        SimpleAsyncTaskExecutor executor =
                new SimpleAsyncTaskExecutor("bank-batch-");
        executor.setConcurrencyLimit(NUMERO_HILOS);
        return executor;
    }
}
