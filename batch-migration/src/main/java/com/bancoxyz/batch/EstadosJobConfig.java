package com.bancoxyz.batch;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

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
public class EstadosJobConfig {

    private final String raizDatos;

    private static final DateTimeFormatter[] FORMATOS = {
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("uuuu/MM/dd")
                .withResolverStyle(ResolverStyle.STRICT),
        DateTimeFormatter.ofPattern("dd-MM-uuuu")
                .withResolverStyle(ResolverStyle.STRICT),
        DateTimeFormatter.ofPattern("dd/MM/uuuu")
                .withResolverStyle(ResolverStyle.STRICT)
    };

    public EstadosJobConfig(
            @Value("${legacy.data.root}") String raizDatos) {
        this.raizDatos = raizDatos;
    }

    record Linea(int numero, String texto) {}

    record Registro(
            String semana,
            int linea,
            Long cuentaId,
            LocalDate fecha,
            String transaccion,
            BigDecimal monto,
            String descripcion,
            String estado,
            String motivo,
            String original) {}

    @Bean
    public Step migrarEstadosSemana1(
            JobRepository repositorio,
            PlatformTransactionManager transacciones,
            DataSource datos) {
        return crearPaso("semana_1", repositorio, transacciones, datos);
    }

    @Bean
    public Step migrarEstadosSemana2(
            JobRepository repositorio,
            PlatformTransactionManager transacciones,
            DataSource datos) {
        return crearPaso("semana_2", repositorio, transacciones, datos);
    }

    @Bean
    public Step migrarEstadosSemana3(
            JobRepository repositorio,
            PlatformTransactionManager transacciones,
            DataSource datos) {
        return crearPaso("semana_3", repositorio, transacciones, datos);
    }

    @Bean
    public Job estadosJob(
            JobRepository repositorio,
            @Qualifier("migrarEstadosSemana1") Step paso1,
            @Qualifier("migrarEstadosSemana2") Step paso2,
            @Qualifier("migrarEstadosSemana3") Step paso3) {

        return new JobBuilder("estadosJob", repositorio)
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
                .name("lectorEstados_" + semana)
                .resource(new FileSystemResource(
                        Path.of(
                                raizDatos,
                                semana,
                                "estados_financieros_anuales.csv")))
                .linesToSkip(1)
                .lineMapper((texto, numero) ->
                        new Linea(numero, texto))
                .build();

        JdbcTemplate jdbc = new JdbcTemplate(datos);

        ItemWriter<Registro> escritor = bloque -> {

            String sql = """
                INSERT INTO legacy_estados
                (semana, linea, cuenta_id, fecha, transaccion,
                 monto, descripcion, estado, motivo, texto_original)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    cuenta_id = VALUES(cuenta_id),
                    fecha = VALUES(fecha),
                    transaccion = VALUES(transaccion),
                    monto = VALUES(monto),
                    descripcion = VALUES(descripcion),
                    estado = VALUES(estado),
                    motivo = VALUES(motivo),
                    texto_original = VALUES(texto_original)
                """;

            for (Registro r : bloque.getItems()) {
                jdbc.update(
                        sql,
                        r.semana(),
                        r.linea(),
                        r.cuentaId(),
                        r.fecha() == null
                                ? null : Date.valueOf(r.fecha()),
                        r.transaccion(),
                        r.monto(),
                        r.descripcion(),
                        r.estado(),
                        r.motivo(),
                        r.original()
                );
            }
        };

        return new StepBuilder(
                "migrarEstados_" + semana, repositorio)
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
            String[] campos = linea.texto().split(",", 5);

            if (campos.length != 5) {
                throw new IllegalArgumentException(
                        "Cantidad de columnas incorrecta");
            }

            long cuentaId = Long.parseLong(campos[0].trim());

            if (cuentaId <= 0) {
                throw new IllegalArgumentException(
                        "Cuenta invalida");
            }

            LocalDate fecha = convertirFecha(campos[1].trim());

            String tipo = campos[2].trim()
                    .toLowerCase(Locale.ROOT)
                    .replace("ó", "o");

            if (!tipo.equals("deposito")
                    && !tipo.equals("retiro")
                    && !tipo.equals("compra")
                    && !tipo.equals("pago")) {
                throw new IllegalArgumentException(
                        "Tipo de transaccion invalido");
            }

            if (campos[3].trim().isBlank()) {
                throw new IllegalArgumentException(
                        "Monto faltante");
            }

            BigDecimal monto =
                    new BigDecimal(campos[3].trim());

            if (monto.signum() <= 0) {
                throw new IllegalArgumentException(
                        "Monto negativo o cero");
            }

            if (monto.scale() > 2 || monto.precision() > 18) {
                throw new IllegalArgumentException(
                        "Monto fuera del formato permitido");
            }

            String descripcion = campos[4].trim();

            if (descripcion.isBlank()) {
                throw new IllegalArgumentException(
                        "Descripcion vacia");
            }

            if (descripcion.length() > 255) {
                throw new IllegalArgumentException(
                        "Descripcion demasiado larga");
            }

            return new Registro(
                    semana,
                    linea.numero(),
                    cuentaId,
                    fecha,
                    tipo,
                    monto,
                    descripcion,
                    "VALIDO",
                    null,
                    linea.texto()
            );

        } catch (RuntimeException error) {

            String motivo = error.getMessage();

            if (motivo == null || motivo.isBlank()) {
                motivo = "Registro invalido";
            }

            if (motivo.length() > 250) {
                motivo = motivo.substring(0, 250);
            }

            return new Registro(
                    semana,
                    linea.numero(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    "RECHAZADO",
                    motivo,
                    linea.texto()
            );
        }
    }

    private LocalDate convertirFecha(String texto) {

        for (DateTimeFormatter formato : FORMATOS) {
            try {
                return LocalDate.parse(texto, formato);
            } catch (DateTimeParseException error) {
                // Probar el siguiente formato conocido.
            }
        }

        throw new IllegalArgumentException(
                "Fecha incorrecta o no reconocida");
    }
}
