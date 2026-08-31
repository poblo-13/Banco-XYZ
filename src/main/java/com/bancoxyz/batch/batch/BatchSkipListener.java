package com.bancoxyz.batch.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

/**
 * Registra los elementos omitidos por la política de tolerancia a fallos
 * y mantiene un archivo de auditoría para posible reproceso.
 */
@Component
public class BatchSkipListener implements SkipListener<Object, Object> {

    private static final Logger logger =
            LoggerFactory.getLogger(BatchSkipListener.class);

    private static final Path ARCHIVO_RECHAZADOS =
            Path.of("output", "registros_rechazados.csv");

    @Override
    public void onSkipInRead(Throwable throwable) {

        logger.warn(
                "[SKIP][READ] {}",
                throwable.getMessage());

        registrarRechazo(
                "READ",
                "",
                throwable);
    }

    @Override
    public void onSkipInWrite(
            Object item,
            Throwable throwable) {

        logger.warn(
                "[SKIP][WRITE] {} | item={}",
                throwable.getMessage(),
                item);

        registrarRechazo(
                "WRITE",
                item,
                throwable);
    }

    @Override
    public void onSkipInProcess(
            Object item,
            Throwable throwable) {

        logger.warn(
                "[SKIP][PROCESS] {} | item={}",
                throwable.getMessage(),
                item);

        registrarRechazo(
                "PROCESS",
                item,
                throwable);
    }

    /**
     * Guarda el registro rechazado en un CSV.
     * El método synchronized evita escrituras simultáneas
     * cuando el Step trabaja con múltiples hilos.
     */
    private synchronized void registrarRechazo(
            String fase,
            Object item,
            Throwable throwable) {

        try {

            Files.createDirectories(
                    ARCHIVO_RECHAZADOS.getParent());

            boolean escribirCabecera =
                    !Files.exists(ARCHIVO_RECHAZADOS)
                            || Files.size(ARCHIVO_RECHAZADOS) == 0;

            try (BufferedWriter writer =
                         Files.newBufferedWriter(
                                 ARCHIVO_RECHAZADOS,
                                 StandardCharsets.UTF_8,
                                 StandardOpenOption.CREATE,
                                 StandardOpenOption.APPEND)) {

                if (escribirCabecera) {

                    writer.write(
                            "fecha_hora,fase,item,error");

                    writer.newLine();
                }

                writer.write(
                        escaparCsv(
                                LocalDateTime.now().toString()));

                writer.write(",");

                writer.write(
                        escaparCsv(fase));

                writer.write(",");

                writer.write(
                        escaparCsv(
                                item == null
                                        ? ""
                                        : item.toString()));

                writer.write(",");

                writer.write(
                        escaparCsv(
                                throwable == null
                                        ? ""
                                        : throwable.getMessage()));

                writer.newLine();
            }

        } catch (IOException exception) {

            logger.error(
                    "No fue posible registrar el elemento rechazado "
                            + "en el archivo de auditoría.",
                    exception);
        }
    }

    /**
     * Escapa valores para evitar problemas con comas,
     * saltos de línea o comillas dentro del CSV.
     */
    private String escaparCsv(String valor) {

        if (valor == null) {
            return "";
        }

        String limpio =
                valor.replace("\"", "\"\"")
                        .replace("\r", " ")
                        .replace("\n", " ");

        return "\"" + limpio + "\"";
    }
}