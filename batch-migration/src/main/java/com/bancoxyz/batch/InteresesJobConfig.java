package com.bancoxyz.batch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;

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
public class InteresesJobConfig {

    private final String raizDatos;
    private final BigDecimal tasaAhorro;
    private final BigDecimal tasaPrestamo;
    private final BigDecimal tasaHipoteca;

    public InteresesJobConfig(
            @Value("${legacy.data.root}") String raizDatos,
            @Value("${batch.intereses.tasa-anual.ahorro:0.04}")
                    BigDecimal tasaAhorro,
            @Value("${batch.intereses.tasa-anual.prestamo:0.12}")
                    BigDecimal tasaPrestamo,
            @Value("${batch.intereses.tasa-anual.hipoteca:0.08}")
                    BigDecimal tasaHipoteca) {

        this.raizDatos = raizDatos;
        this.tasaAhorro = tasaAhorro;
        this.tasaPrestamo = tasaPrestamo;
        this.tasaHipoteca = tasaHipoteca;
    }

    record Linea(int numero, String texto) {}

    record Registro(
            String semana,
            int linea,
            Long cuentaId,
            String nombre,
            BigDecimal saldo,
            Integer edad,
            String tipo,
            BigDecimal interesCalculado,
            String estado,
            String motivo,
            String textoOriginal) {}

    @Bean
    public Step migrarInteresesSemana1(
            JobRepository repositorio,
            PlatformTransactionManager transacciones,
            DataSource datos) {

        return crearPaso("semana_1", repositorio, transacciones, datos);
    }

    @Bean
    public Step migrarInteresesSemana2(
            JobRepository repositorio,
            PlatformTransactionManager transacciones,
            DataSource datos) {

        return crearPaso("semana_2", repositorio, transacciones, datos);
    }

    @Bean
    public Step migrarInteresesSemana3(
            JobRepository repositorio,
            PlatformTransactionManager transacciones,
            DataSource datos) {

        return crearPaso("semana_3", repositorio, transacciones, datos);
    }

    @Bean
    public Job interesesJob(
            JobRepository repositorio,
            @Qualifier("migrarInteresesSemana1") Step paso1,
            @Qualifier("migrarInteresesSemana2") Step paso2,
            @Qualifier("migrarInteresesSemana3") Step paso3) {

        return ParallelBatchFlows.crear("interesesJob", repositorio, paso1, paso2, paso3);
    }

    private Step crearPaso(
            String semana,
            JobRepository repositorio,
            PlatformTransactionManager transacciones,
            DataSource datos) {

        var lector = new FlatFileItemReaderBuilder<Linea>()
                .name("lectorIntereses_" + semana)
                .resource(new FileSystemResource(
                        Path.of(
                                raizDatos,
                                semana,
                                "intereses_trimestrales.csv")))
                .linesToSkip(1)
                .lineMapper((texto, numero) ->
                        new Linea(numero, texto))
                .build();

        JdbcTemplate jdbc = new JdbcTemplate(datos);

        ItemWriter<Registro> escritor = bloque -> {

            String sql = """
                INSERT INTO legacy_intereses
                (semana, linea, cuenta_id, nombre, saldo, edad,
                 tipo, interes_calculado, estado, motivo, texto_original)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    cuenta_id = VALUES(cuenta_id),
                    nombre = VALUES(nombre),
                    saldo = VALUES(saldo),
                    edad = VALUES(edad),
                    tipo = VALUES(tipo),
                    interes_calculado = VALUES(interes_calculado),
                    estado = VALUES(estado),
                    motivo = VALUES(motivo),
                    texto_original = VALUES(texto_original)
                """;

            for (Registro r : bloque.getItems()) {

                String estado = r.estado();
                String motivo = r.motivo();
                BigDecimal interes = r.interesCalculado();

                // Primera cuenta valida por semana.
                // Una repeticion posterior se conserva como rechazo.
                if ("VALIDO".equals(estado)) {

                    Long anteriores = jdbc.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM legacy_intereses
                            WHERE semana = ?
                              AND cuenta_id = ?
                              AND estado = 'VALIDO'
                              AND linea < ?
                            """,
                            Long.class,
                            r.semana(),
                            r.cuentaId(),
                            r.linea()
                    );

                    if (anteriores != null && anteriores > 0) {
                        estado = "RECHAZADO";
                        motivo = "Cuenta duplicada en la misma semana";
                        interes = null;
                    }
                }

                jdbc.update(
                        sql,
                        r.semana(),
                        r.linea(),
                        r.cuentaId(),
                        r.nombre(),
                        r.saldo(),
                        r.edad(),
                        r.tipo(),
                        interes,
                        estado,
                        motivo,
                        r.textoOriginal()
                );
            }
        };

        return new StepBuilder(
                "migrarIntereses_" + semana, repositorio)
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

    Registro validar(Linea linea, String semana) {

        try {

            String[] campos = linea.texto().split(",", -1);

            if (campos.length != 5) {
                throw new IllegalArgumentException(
                        "Cantidad de columnas incorrecta");
            }

            long cuentaId = Long.parseLong(campos[0].trim());

            if (cuentaId <= 0) {
                throw new IllegalArgumentException(
                        "Identificador de cuenta invalido");
            }

            String nombre = campos[1].trim();

            if (nombre.isBlank() ||
                    nombre.equalsIgnoreCase("unknown")) {
                throw new IllegalArgumentException(
                        "Nombre de titular faltante o desconocido");
            }

            String textoSaldo = campos[2].trim();

            if (textoSaldo.isBlank()) {
                throw new IllegalArgumentException(
                        "Saldo faltante");
            }

            BigDecimal saldo = new BigDecimal(textoSaldo);

            if (saldo.signum() < 0) {
                throw new IllegalArgumentException(
                        "Saldo negativo");
            }

            if (saldo.scale() > 2 || saldo.precision() > 18) {
                throw new IllegalArgumentException(
                        "Saldo fuera del formato permitido");
            }

            String textoEdad = campos[3].trim();

            if (textoEdad.isBlank()) {
                throw new IllegalArgumentException(
                        "Edad faltante");
            }

            int edad = Integer.parseInt(textoEdad);

            // Rango de validacion definido para el prototipo.
            if (edad < 18 || edad > 99) {
                throw new IllegalArgumentException(
                        "Edad fuera del rango permitido");
            }

            String tipo = campos[4].trim().toLowerCase();

            BigDecimal tasaAnual = switch (tipo) {
                case "ahorro" -> tasaAhorro;
                case "prestamo" -> tasaPrestamo;
                case "hipoteca" -> tasaHipoteca;
                default -> throw new IllegalArgumentException(
                        "Tipo de producto financiero invalido");
            };

            if (tasaAnual.signum() < 0) {
                throw new IllegalArgumentException(
                        "Tasa de interes negativa");
            }

            // Interes trimestral simple simulado:
            // saldo * tasa anual / 4
            BigDecimal interes = saldo
                    .multiply(tasaAnual)
                    .divide(
                            BigDecimal.valueOf(4),
                            2,
                            RoundingMode.HALF_UP);

            return new Registro(
                    semana,
                    linea.numero(),
                    cuentaId,
                    nombre,
                    saldo,
                    edad,
                    tipo,
                    interes,
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
                    null,
                    "RECHAZADO",
                    motivo,
                    linea.texto()
            );
        }
    }
}
