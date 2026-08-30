package com.bancoxyz.batch.processor;

import com.bancoxyz.batch.model.CuentaAnualCsv;
import com.bancoxyz.batch.model.MovimientoAnual;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class MovimientoAnualProcessor
        implements ItemProcessor<CuentaAnualCsv, MovimientoAnual> {

    @Override
    public MovimientoAnual process(CuentaAnualCsv item) throws Exception {

        Long cuentaId = Long.parseLong(item.getCuentaId());

        LocalDate fecha = convertirFecha(item.getFecha());

        String transaccion = item.getTransaccion()
                .trim()
                .toLowerCase();

        BigDecimal monto = new BigDecimal(item.getMonto());

        String descripcion = item.getDescripcion();

        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException(
                    "Descripción inválida para cuenta " + cuentaId
            );
        }

        switch (transaccion) {

            case "deposito":
                if (monto.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException(
                            "Depósito inválido para cuenta " + cuentaId
                    );
                }
                break;

            case "retiro":
            case "compra":
                if (monto.compareTo(BigDecimal.ZERO) >= 0) {
                    throw new IllegalArgumentException(
                            "Monto inválido para " + transaccion
                                    + " en cuenta " + cuentaId
                    );
                }
                break;

            default:
                throw new IllegalArgumentException(
                        "Tipo de transacción inválido para cuenta " + cuentaId
                );
        }

        return new MovimientoAnual(
                cuentaId,
                fecha,
                transaccion,
                monto,
                descripcion.trim()
        );
    }

    private LocalDate convertirFecha(String fecha) {

        try {
            return LocalDate.parse(
                    fecha,
                    DateTimeFormatter.ISO_LOCAL_DATE
            );

        } catch (DateTimeParseException e) {

            try {
                return LocalDate.parse(
                        fecha,
                        DateTimeFormatter.ofPattern("yyyy/MM/dd")
                );

            } catch (DateTimeParseException ex) {

                throw new IllegalArgumentException(
                        "Formato de fecha inválido: " + fecha
                );
            }
        }
    }
}