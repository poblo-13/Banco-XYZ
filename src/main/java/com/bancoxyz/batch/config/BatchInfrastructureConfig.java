package com.bancoxyz.batch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;

@Configuration
public class BatchInfrastructureConfig {

    @Value("${batch.core-pool-size}")
    private int corePoolSize;

    @Value("${batch.max-pool-size}")
    private int maxPoolSize;

    @Value("${batch.queue-capacity}")
    private int queueCapacity;

    @Value("${batch.retry-limit}")
    private int retryLimit;

    @Value("${batch.retry-initial-interval}")
    private long retryInitialInterval;

    @Value("${batch.retry-multiplier}")
    private double retryMultiplier;

    @Value("${batch.retry-max-interval}")
    private long retryMaxInterval;

    @Bean
    public ThreadPoolTaskExecutor batchTaskExecutor() {

        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setThreadNamePrefix("bank-batch-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setDaemon(true);

        return executor;
    }

    @Bean
    public RetryPolicy batchRetryPolicy() {

        return RetryPolicy.builder()
                .maxRetries(retryLimit)
                .delay(Duration.ofMillis(retryInitialInterval))
                .multiplier(retryMultiplier)
                .maxDelay(Duration.ofMillis(retryMaxInterval))
                .includes(TransientDataAccessException.class)
                .build();
    }
}