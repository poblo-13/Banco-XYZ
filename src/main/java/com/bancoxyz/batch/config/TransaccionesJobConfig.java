package com.bancoxyz.batch.config;

import com.bancoxyz.batch.batch.BatchSkipListener;
import com.bancoxyz.batch.batch.CustomSkipPolicy;
import com.bancoxyz.batch.model.Transaccion;
import com.bancoxyz.batch.model.TransaccionCsv;
import com.bancoxyz.batch.processor.TransaccionProcessor;
import com.bancoxyz.batch.repository.TransaccionRepository;

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
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Configuration
public class TransaccionesJobConfig {

    @Value("${batch.chunk-size}")
    private int chunkSize;

    @Value("${batch.max-skips}")
    private int maxSkips;

    @Bean
    public FlatFileItemReader<TransaccionCsv> transaccionFileReader() {

        return new FlatFileItemReaderBuilder<TransaccionCsv>()
                .name("transaccionFileReader")
                .resource(new ClassPathResource("data/transacciones.csv"))
                .linesToSkip(1)
                .delimited()
                .names("id", "fecha", "monto", "tipo")
                .targetType(TransaccionCsv.class)
                .build();
    }

    @Bean
    public SynchronizedItemStreamReader<TransaccionCsv> transaccionReader(
            FlatFileItemReader<TransaccionCsv> transaccionFileReader) {

        return new SynchronizedItemStreamReader<>(
                transaccionFileReader);
    }

    @Bean
    public RepositoryItemWriter<Transaccion> transaccionWriter(
            TransaccionRepository repository) {

        return new RepositoryItemWriterBuilder<Transaccion>()
                .repository(repository)
                .methodName("save")
                .build();
    }

    @Bean
    public Step limpiarTransaccionesStep(
            JobRepository jobRepository,
            TransaccionRepository repository) {

        return new StepBuilder(
                "limpiarTransaccionesStep",
                jobRepository)
                .tasklet((contribution, chunkContext) -> {

                    repository.deleteAllInBatch();

                    System.out.println(
                            "[CLEANUP] Registros anteriores eliminados.");

                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step transaccionesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            SynchronizedItemStreamReader<TransaccionCsv> transaccionReader,
            TransaccionProcessor processor,
            RepositoryItemWriter<Transaccion> transaccionWriter,
            ThreadPoolTaskExecutor batchTaskExecutor,
            BatchSkipListener skipListener,
            RetryPolicy batchRetryPolicy) {

        return new StepBuilder(
                "transaccionesStep",
                jobRepository)
                .<TransaccionCsv, Transaccion>chunk(chunkSize)
                .transactionManager(transactionManager)
                .reader(transaccionReader)
                .processor(processor)
                .writer(transaccionWriter)
                .faultTolerant()
                .retryPolicy(batchRetryPolicy)
                .skipPolicy(new CustomSkipPolicy(maxSkips))
                .skipListener(skipListener)
                .taskExecutor(batchTaskExecutor)
                .build();
    }

    @Bean
    public Step resumenTransaccionesStep(
            JobRepository jobRepository,
            TransaccionRepository repository) {

        return new StepBuilder(
                "resumenTransaccionesStep",
                jobRepository)
                .tasklet((contribution, chunkContext) -> {

                    List<Transaccion> transacciones =
                            repository.findAll();

                    long total =
                            transacciones.size();

                    long debitos =
                            transacciones.stream()
                                    .filter(t ->
                                            "debito".equals(t.getTipo()))
                                    .count();

                    long creditos =
                            transacciones.stream()
                                    .filter(t ->
                                            "credito".equals(t.getTipo()))
                                    .count();

                    BigDecimal montoTotal =
                            transacciones.stream()
                                    .map(Transaccion::getMonto)
                                    .reduce(
                                            BigDecimal.ZERO,
                                            BigDecimal::add);

                    Map<java.time.LocalDate, List<Transaccion>> porFecha =
                            transacciones.stream()
                                    .collect(
                                            Collectors.groupingBy(
                                                    Transaccion::getFecha,
                                                    TreeMap::new,
                                                    Collectors.toList()));

                    System.out.println(
                            "====================================");

                    System.out.println(
                            " REPORTE DE TRANSACCIONES DIARIAS");

                    System.out.println(
                            "====================================");

                    System.out.println(
                            "Registros válidos almacenados: "
                                    + total);

                    System.out.println(
                            "Débitos: " + debitos);

                    System.out.println(
                            "Créditos: " + creditos);

                    System.out.println(
                            "Monto total: $" + montoTotal);

                    System.out.println(
                            "------------------------------------");

                    porFecha.forEach(
                            (fecha, lista) ->
                                    System.out.println(
                                            fecha
                                                    + " | movimientos="
                                                    + lista.size()));

                    System.out.println(
                            "====================================");

                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Job transaccionesJob(
            JobRepository jobRepository,
            Step limpiarTransaccionesStep,
            Step transaccionesStep,
            Step resumenTransaccionesStep) {

        return new JobBuilder(
                "transaccionesJob",
                jobRepository)
                .start(limpiarTransaccionesStep)
                .next(transaccionesStep)
                .next(resumenTransaccionesStep)
                .build();
    }
}