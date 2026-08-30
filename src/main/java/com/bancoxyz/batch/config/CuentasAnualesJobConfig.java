package com.bancoxyz.batch.config;

import com.bancoxyz.batch.batch.BatchSkipListener;
import com.bancoxyz.batch.batch.CustomSkipPolicy;
import com.bancoxyz.batch.model.CuentaAnualCsv;
import com.bancoxyz.batch.model.MovimientoAnual;
import com.bancoxyz.batch.processor.MovimientoAnualProcessor;
import com.bancoxyz.batch.repository.MovimientoAnualRepository;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class CuentasAnualesJobConfig {

    @Bean
    public FlatFileItemReader<CuentaAnualCsv> movimientoAnualFileReader() {
        return new FlatFileItemReaderBuilder<CuentaAnualCsv>()
                .name("movimientoAnualFileReader")
                .resource(new ClassPathResource("data/cuentas_anuales.csv"))
                .linesToSkip(1)
                .delimited()
                .names("cuentaId", "fecha", "transaccion", "monto", "descripcion")
                .targetType(CuentaAnualCsv.class)
                .build();
    }

    @Bean
    public SynchronizedItemStreamReader<CuentaAnualCsv> movimientoAnualReader(
            FlatFileItemReader<CuentaAnualCsv> movimientoAnualFileReader) {
        return new SynchronizedItemStreamReader<>(movimientoAnualFileReader);
    }

    @Bean
    public RepositoryItemWriter<MovimientoAnual> movimientoAnualWriter(
            MovimientoAnualRepository repository) {
        return new RepositoryItemWriterBuilder<MovimientoAnual>()
                .repository(repository)
                .methodName("save")
                .build();
    }

    @Bean
    public Step limpiarMovimientosAnualesStep(
            JobRepository jobRepository,
            MovimientoAnualRepository repository) {
        return new StepBuilder("limpiarMovimientosAnualesStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    repository.deleteAllInBatch();
                    System.out.println("[CLEANUP] Movimientos anuales anteriores eliminados.");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step movimientosAnualesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            SynchronizedItemStreamReader<CuentaAnualCsv> movimientoAnualReader,
            MovimientoAnualProcessor processor,
            RepositoryItemWriter<MovimientoAnual> movimientoAnualWriter,
            SimpleAsyncTaskExecutor batchTaskExecutor,
            BatchSkipListener skipListener) {

        return new StepBuilder("movimientosAnualesStep", jobRepository)
                .<CuentaAnualCsv, MovimientoAnual>chunk(
                        BatchInfrastructureConfig.CHUNK_SIZE)
                .transactionManager(transactionManager)
                .reader(movimientoAnualReader)
                .processor(processor)
                .writer(movimientoAnualWriter)
                .faultTolerant()
                .skipPolicy(new CustomSkipPolicy(
                        BatchInfrastructureConfig.MAX_SKIPS))
                .skipListener(skipListener)
                .taskExecutor(batchTaskExecutor)
                .build();
    }

    @Bean
    public Step reporteAnualStep(
            JobRepository jobRepository,
            MovimientoAnualRepository repository) {

        return new StepBuilder("reporteAnualStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {

                    List<MovimientoAnual> movimientos =
                            repository.findAllByOrderByCuentaIdAscFechaAsc();

                    Path rutaReporte = Path.of(
                            "output",
                            "reporte_anual_auditoria.csv");

                    Files.createDirectories(rutaReporte.getParent());

                    Map<Long, BigDecimal> saldosAcumulados = new HashMap<>();

                    try (BufferedWriter writer = Files.newBufferedWriter(
                            rutaReporte,
                            StandardCharsets.UTF_8)) {

                        writer.write(
                                "cuenta_id,fecha,transaccion,monto,"
                                        + "descripcion,saldo_movimientos_acumulado");
                        writer.newLine();

                        for (MovimientoAnual movimiento : movimientos) {

                            BigDecimal saldoAcumulado =
                                    saldosAcumulados.getOrDefault(
                                            movimiento.getCuentaId(),
                                            BigDecimal.ZERO)
                                            .add(movimiento.getMonto());

                            saldosAcumulados.put(
                                    movimiento.getCuentaId(),
                                    saldoAcumulado);

                            writer.write(
                                    movimiento.getCuentaId() + ","
                                            + movimiento.getFecha() + ","
                                            + movimiento.getTransaccion() + ","
                                            + movimiento.getMonto() + ","
                                            + movimiento.getDescripcion() + ","
                                            + saldoAcumulado);
                            writer.newLine();
                        }
                    }

                    System.out.println("====================================");
                    System.out.println(" ESTADOS DE CUENTA ANUALES");
                    System.out.println("====================================");
                    System.out.println(
                            "Movimientos incluidos: " + movimientos.size());
                    System.out.println(
                            "Archivo generado: "
                                    + rutaReporte.toAbsolutePath());
                    System.out.println("====================================");

                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Job cuentasAnualesJob(
            JobRepository jobRepository,
            Step limpiarMovimientosAnualesStep,
            Step movimientosAnualesStep,
            Step reporteAnualStep) {

        return new JobBuilder("cuentasAnualesJob", jobRepository)
                .start(limpiarMovimientosAnualesStep)
                .next(movimientosAnualesStep)
                .next(reporteAnualStep)
                .build();
    }
}
