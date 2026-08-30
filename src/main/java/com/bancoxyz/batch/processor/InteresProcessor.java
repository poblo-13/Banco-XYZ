package com.bancoxyz.batch.processor;

import com.bancoxyz.batch.model.InteresCalculado;
import com.bancoxyz.batch.model.InteresCsv;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class InteresProcessor
        implements ItemProcessor<InteresCsv, InteresCalculado> {

    private static final BigDecimal TASA_AHORRO =
            new BigDecimal("0.005");

    private static final BigDecimal TASA_PRESTAMO =
            new BigDecimal("0.015");

    @Override
    public InteresCalculado process(InteresCsv item) throws Exception {

        Long cuentaId = Long.parseLong(item.getCuentaId());

        String nombre = item.getNombre();

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "Nombre inválido para cuenta " + cuentaId
            );
        }

        BigDecimal saldo = new BigDecimal(item.getSaldo());

        if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Saldo inválido para cuenta " + cuentaId
            );
        }

        Integer edad = Integer.parseInt(item.getEdad());

        if (edad <= 0 || edad > 120) {
            throw new IllegalArgumentException(
                    "Edad inválida para cuenta " + cuentaId
            );
        }

        String tipo = item.getTipo().trim().toLowerCase();

        // Las hipotecas no forman parte de este proceso mensual
        if (tipo.equals("hipoteca")) {
            return null;
        }

        BigDecimal tasa;

        if (tipo.equals("ahorro")) {
            tasa = TASA_AHORRO;
        } else if (tipo.equals("prestamo")) {
            tasa = TASA_PRESTAMO;
        } else {
            throw new IllegalArgumentException(
                    "Tipo de cuenta inválido para cuenta " + cuentaId
            );
        }

        BigDecimal interesCalculado = saldo
                .multiply(tasa)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal saldoFinal = saldo
                .add(interesCalculado)
                .setScale(2, RoundingMode.HALF_UP);

        return new InteresCalculado(
                cuentaId,
                nombre.trim(),
                saldo,
                edad,
                tipo,
                tasa,
                interesCalculado,
                saldoFinal
        );
    }
}