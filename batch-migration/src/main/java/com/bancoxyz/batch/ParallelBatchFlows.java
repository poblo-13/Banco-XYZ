package com.bancoxyz.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

final class ParallelBatchFlows {

    private ParallelBatchFlows() {
    }

    static Job crear(
            String nombre,
            JobRepository repositorio,
            Step paso1,
            Step paso2,
            Step paso3) {

        // Maximo tres flujos simultaneos
        SimpleAsyncTaskExecutor executor =
                new SimpleAsyncTaskExecutor("batch-" + nombre + "-");

        executor.setConcurrencyLimit(3);

        Flow semana1 = new FlowBuilder<SimpleFlow>(
                nombre + "-flujo-semana1")
                .start(paso1)
                .build();

        Flow semana2 = new FlowBuilder<SimpleFlow>(
                nombre + "-flujo-semana2")
                .start(paso2)
                .build();

        Flow semana3 = new FlowBuilder<SimpleFlow>(
                nombre + "-flujo-semana3")
                .start(paso3)
                .build();

        Flow paralelo = new FlowBuilder<SimpleFlow>(
                nombre + "-paralelo")
                .start(semana1)
                .split(executor)
                .add(semana2, semana3)
                .build();

        return new JobBuilder(nombre, repositorio)
                .start(paralelo)
                .end()
                .build();
    }
}
