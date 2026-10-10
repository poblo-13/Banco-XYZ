package com.bancoxyz.batch;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.sql.DataSource;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class MovimientosJobConfig {

    private final String raizDatos;

    public MovimientosJobConfig(
            @Value("${legacy.data.root}") String raizDatos) {
        this.raizDatos = raizDatos;
    }

    record Linea(int numero, String texto) {}

    record Registro(
            String semana,
            int linea,
            Long idOrigen,
            LocalDate fecha,
            BigDecimal monto,
            String tipo,
            String estado,
            String motivo,
            String textoOriginal) {}

    @Bean
    public Step migrarMovimientosSemana1(
            JobRepository repositorio,
            PlatformTransactionManager transacciones,
            DataSource datos) {
        return crearPaso("semana_1", repositorio, transacciones, datos);
    }

    @Bean
    public Step migrarMovimientosSemana2(
            JobRepository repositorio,
            PlatformTransactionManager transacciones,
            DataSource datos) {
        return crearPaso("semana_2", repositorio, transacciones, datos);
    }

    @Bean
    public Step migrarMovimientosSemana3(
            JobRepository repositorio,
            PlatformTransactionManager transacciones,
            DataSource datos) {
        return crearPaso("semana_3", repositorio, transacciones, datos);
    }

    @Bean
    public Job movimientosJob(
            JobRepository repositorio,
            @Qualifier("migrarMovimientosSemana1") Step paso1,
            @Qualifier("migrarMovimientosSemana2") Step paso2,
            @Qualifier("migrarMovimientosSemana3") Step paso3) {

        return new JobBuilder("movimientosJob", repositorio)
                .start(paso1)
                .next(paso2)
                .next(paso3)
                .build();
    }

    private Step crearPaso(
            String semana,
            JobRepository repositorio,
            PlatformTransactionManager transacciones,
            DataSource datos) {

        var lector = new FlatFileItemReaderBuilder<Linea>()
                .name("lectorMovimientos_" + semana)
                .resource(new FileSystemResource(
                        Path.of(raizDatos, semana,
                                "movimientos_financieros_diarios.csv")))
                .linesToSkip(1)
                .lineMapper((texto, numero) ->
                        new Linea(numero, texto))
                .build();

        JdbcTemplate jdbc = new JdbcTemplate(datos);

        ItemWriter<Registro> escritor = bloque -> {

            String sql = """
                INSERT INTO legacy_movimientos
                (semana, linea, id_origen, fecha, monto, tipo,
                 estado, motivo, texto_original)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    id_origen = VALUES(id_origen),
                    fecha = VALUES(fecha),
                    monto = VALUES(monto),
                    tipo = VALUES(tipo),
                    estado = VALUES(estado),
                    motivo = VALUES(motivo),
                    texto_original = VALUES(texto_original)
                """;

            for (Registro r : bloque.getItems()) {
                jdbc.update(
                        sql,
                        r.semana(),
                        r.linea(),
                        r.idOrigen(),
                        r.fecha() == null
                                ? null : Date.valueOf(r.fecha()),
                        r.monto(),
                        r.tipo(),
                        r.estado(),
                        r.motivo(),
                        r.textoOriginal()
                );
            }
        };

        return new StepBuilder(
                "migrarMovimientos_" + semana, repositorio)
                .<Linea, Registro>chunk(50)
                .transactionManager(transacciones)
                .reader(lector)
                .processor(linea -> validar(linea, semana))
                .writer(escritor)
                .faultTolerant()
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .build();
    }

    private Registro validar(Linea linea, String semana) {

        try {
            String[] campos = linea.texto().split(",", -1);

            if (campos.length != 4) {
                throw new IllegalArgumentException(
                        "Cantidad de columnas incorrecta");
            }

            long id = Long.parseLong(campos[0].trim());

            if (id <= 0) {
                throw new IllegalArgumentException(
                        "ID de movimiento invalido");
            }

            LocalDate fecha = LocalDate.parse(
                    campos[1].trim(),
                    DateTimeFormatter.ISO_LOCAL_DATE);

            String textoMonto = campos[2].trim();

            if (textoMonto.isEmpty()) {
                throw new IllegalArgumentException(
                        "Monto faltante");
            }

            BigDecimal monto = new BigDecimal(textoMonto);

            if (monto.signum() <= 0) {
                throw new IllegalArgumentException(
                        "Monto negativo o cero");
            }

            String tipo = campos[3].trim().toLowerCase();

            if (!tipo.equals("credito") &&
                    !tipo.equals("debito")) {
                throw new IllegalArgumentException(
                        "Tipo de movimiento invalido");
            }

            return new Registro(
                    semana, linea.numero(), id, fecha,
                    monto, tipo, "VALIDO", null, linea.texto());

        } catch (RuntimeException error) {

            String motivo = error.getMessage();

            if (motivo == null) {
                motivo = "Registro invalido";
            }

            if (motivo.length() > 250) {
                motivo = motivo.substring(0, 250);
            }

            return new Registro(
                    semana, linea.numero(), null, null,
                    null, null, "RECHAZADO",
                    motivo, linea.texto());
        }
    }
}
