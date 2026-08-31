package com.bancoxyz.batch.config;

import com.bancoxyz.batch.batch.BatchSkipListener;
import com.bancoxyz.batch.batch.CustomSkipPolicy;
import com.bancoxyz.batch.model.InteresCalculado;
import com.bancoxyz.batch.model.InteresCsv;
import com.bancoxyz.batch.processor.InteresProcessor;
import com.bancoxyz.batch.repository.InteresCalculadoRepository;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.data.RepositoryItemWriter;
import org.springframework.batch.infrastructure.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class InteresesJobConfig {

    @Value("${batch.chunk-size}")
    private int chunkSize;

    @Value("${batch.max-skips}")
    private int maxSkips;

    @Bean
    public FlatFileItemReader<InteresCsv> interesFileReader() {

        return new FlatFileItemReaderBuilder<InteresCsv>()
                .name("interesFileReader")
                .resource(new ClassPathResource("data/intereses.csv"))
                .linesToSkip(1)
                .delimited()
                .names("cuentaId", "nombre", "saldo", "edad", "tipo")
                .targetType(InteresCsv.class)
                .build();
    }

    @Bean
    public SynchronizedItemStreamReader<InteresCsv> interesReader(
            FlatFileItemReader<InteresCsv> interesFileReader) {

        return new SynchronizedItemStreamReader<>(interesFileReader);
    }

    @Bean
    public RepositoryItemWriter<InteresCalculado> interesWriter(
            InteresCalculadoRepository repository) {

        return new RepositoryItemWriterBuilder<InteresCalculado>()
                .repository(repository)
                .methodName("save")
                .build();
    }

    @Bean
    public Step limpiarInteresesStep(
            JobRepository jobRepository,
            InteresCalculadoRepository repository) {

        return new StepBuilder(
                "limpiarInteresesStep",
                jobRepository)
                .tasklet((contribution, chunkContext) -> {

                    repository.deleteAllInBatch();

                    System.out.println(
                            "[CLEANUP] Intereses anteriores eliminados.");

                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step interesesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            SynchronizedItemStreamReader<InteresCsv> interesReader,
            InteresProcessor processor,
            RepositoryItemWriter<InteresCalculado> interesWriter,
            ThreadPoolTaskExecutor batchTaskExecutor,
            BatchSkipListener skipListener,
            RetryPolicy batchRetryPolicy) {

        return new StepBuilder(
                "interesesStep",
                jobRepository)
                .<InteresCsv, InteresCalculado>chunk(chunkSize)
                .transactionManager(transactionManager)
                .reader(interesReader)
                .processor(processor)
                .writer(interesWriter)
                .faultTolerant()
                .retryPolicy(batchRetryPolicy)
                .skipPolicy(new CustomSkipPolicy(maxSkips))
                .skipListener(skipListener)
                .taskExecutor(batchTaskExecutor)
                .build();
    }

    @Bean
    public Step resumenInteresesStep(
            JobRepository jobRepository,
            InteresCalculadoRepository repository) {

        return new StepBuilder(
                "resumenInteresesStep",
                jobRepository)
                .tasklet((contribution, chunkContext) -> {

                    List<InteresCalculado> resultados =
                            repository.findAll();

                    long ahorro =
                            resultados.stream()
                                    .filter(i ->
                                            "ahorro".equals(i.getTipo()))
                                    .count();

                    long prestamo =
                            resultados.stream()
                                    .filter(i ->
                                            "prestamo".equals(i.getTipo()))
                                    .count();

                    BigDecimal interesTotal =
                            resultados.stream()
                                    .map(InteresCalculado::getInteresCalculado)
                                    .reduce(
                                            BigDecimal.ZERO,
                                            BigDecimal::add);

                    System.out.println(
                            "====================================");

                    System.out.println(
                            " CÁLCULO DE INTERESES MENSUALES");

                    System.out.println(
                            "====================================");

                    System.out.println(
                            "Cuentas procesadas: "
                                    + resultados.size());

                    System.out.println(
                            "Cuentas de ahorro: "
                                    + ahorro);

                    System.out.println(
                            "Cuentas de préstamo: "
                                    + prestamo);

                    System.out.println(
                            "Interés total calculado: $"
                                    + interesTotal);

                    System.out.println(
                            "------------------------------------");

                    resultados.forEach(
                            i -> System.out.println(
                                    "Cuenta "
                                            + i.getCuentaId()
                                            + " | tipo="
                                            + i.getTipo()
                                            + " | saldo final=$"
                                            + i.getSaldoFinal()));

                    System.out.println(
                            "====================================");

                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Job interesesJob(
            JobRepository jobRepository,
            Step limpiarInteresesStep,
            Step interesesStep,
            Step resumenInteresesStep) {

        return new JobBuilder(
                "interesesJob",
                jobRepository)
                .start(limpiarInteresesStep)
                .next(interesesStep)
                .next(resumenInteresesStep)
                .build();
    }
}