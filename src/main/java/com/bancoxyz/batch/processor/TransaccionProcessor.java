package com.bancoxyz.batch.processor;

import com.bancoxyz.batch.model.Transaccion;
import com.bancoxyz.batch.model.TransaccionCsv;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TransaccionProcessor
        implements ItemProcessor<TransaccionCsv, Transaccion> {

    /**
     * ConcurrentHashMap permite detectar duplicados de forma segura cuando
     * el Step procesa chunks en paralelo.
     */
    private final Set<String> registrosProcesados =
            ConcurrentHashMap.newKeySet();

    @Override
    public Transaccion process(TransaccionCsv item) {

        Long id = parsearLong(item.getId(), "ID de transacción");

        BigDecimal monto = parsearMonto(item.getMonto());

        if (monto.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException(
                    "Monto cero para transacción ID " + id);
        }

        String tipo = normalizar(item.getTipo());

        if (!tipo.equals("debito") && !tipo.equals("credito")) {
            throw new IllegalArgumentException(
                    "Tipo de transacción inválido para ID " + id);
        }

        /*
         * Corrección de calidad de datos:
         * un débito negativo se normaliza a positivo para mantener una
         * representación consistente del monto procesado.
         */
        if (tipo.equals("debito") && monto.compareTo(BigDecimal.ZERO) < 0) {
            monto = monto.abs();
            System.out.println(
                    "[CORRECCION] Débito negativo normalizado. ID=" + id);
        }

        // Un crédito negativo no se corrige automáticamente: se considera inválido.
        if (tipo.equals("credito") && monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Crédito con monto negativo para ID " + id);
        }

        LocalDate fecha = convertirFecha(item.getFecha());

        String claveDuplicado = fecha + "|" + monto + "|" + tipo;

        if (!registrosProcesados.add(claveDuplicado)) {
            throw new IllegalArgumentException(
                    "Transacción duplicada detectada para ID " + id);
        }

        return new Transaccion(id, fecha, monto, tipo);
    }

    private Long parsearLong(String valor, String campo) {
        try {
            return Long.parseLong(valor.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException(campo + " inválido: " + valor);
        }
    }

    private BigDecimal parsearMonto(String valor) {
        try {
            return new BigDecimal(valor.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Monto inválido: " + valor);
        }
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Tipo de transacción vacío");
        }
        return valor.trim().toLowerCase();
    }

    private LocalDate convertirFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            throw new IllegalArgumentException("Fecha de transacción vacía");
        }

        try {
            return LocalDate.parse(
                    fecha.trim(),
                    DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(
                        fecha.trim(),
                        DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException(
                        "Formato de fecha inválido: " + fecha);
            }
        }
    }
}
