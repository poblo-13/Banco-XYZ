package com.bancoxyz.batch;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidacionesBatchTest {

    private final MovimientosJobConfig movimientos =
            new MovimientosJobConfig("datos");

    private final InteresesJobConfig intereses =
            new InteresesJobConfig(
                    "datos",
                    new BigDecimal("0.04"),
                    new BigDecimal("0.12"),
                    new BigDecimal("0.08"));

    private final EstadosJobConfig estados =
            new EstadosJobConfig("datos");

    @Test
    void movimientoValido() {
        var r = movimientos.validar(
                new MovimientosJobConfig.Linea(
                        2, "1,2024-01-01,1000,debito"),
                "semana_1");

        assertEquals("VALIDO", r.estado());
        assertEquals(1L, r.idOrigen());
        assertEquals(new BigDecimal("1000"), r.monto());
    }

    @Test
    void movimientoNegativoSeRechaza() {
        var r = movimientos.validar(
                new MovimientosJobConfig.Linea(
                        3, "2,2024-01-01,-200,debito"),
                "semana_1");

        assertEquals("RECHAZADO", r.estado());
        assertEquals("Monto negativo o cero", r.motivo());
    }

    @Test
    void movimientoSinMontoSeRechaza() {
        var r = movimientos.validar(
                new MovimientosJobConfig.Linea(
                        4, "3,2024-01-01,,credito"),
                "semana_2");

        assertEquals("RECHAZADO", r.estado());
        assertEquals("Monto faltante", r.motivo());
    }

    @Test
    void movimientoConFechaImposibleSeRechaza() {
        var r = movimientos.validar(
                new MovimientosJobConfig.Linea(
                        5, "4,2024-13-01,500,debito"),
                "semana_3");

        assertEquals("RECHAZADO", r.estado());
    }

    @Test
    void interesTrimestralDeAhorroCorrecto() {
        var r = intereses.validar(
                new InteresesJobConfig.Linea(
                        2, "101,John Doe,5000,30,ahorro"),
                "semana_1");

        assertEquals("VALIDO", r.estado());
        assertEquals(
                0,
                new BigDecimal("50.00")
                        .compareTo(r.interesCalculado()));
    }

    @Test
    void saldoCeroEsValido() {
        var r = intereses.validar(
                new InteresesJobConfig.Linea(
                        3, "104,Alice Brown,0,45,ahorro"),
                "semana_1");

        assertEquals("VALIDO", r.estado());
        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(
                        r.interesCalculado()));
    }

    @Test
    void edadFaltanteSeRechaza() {
        var r = intereses.validar(
                new InteresesJobConfig.Linea(
                        4, "103,Bob Johnson,12000,,prestamo"),
                "semana_2");

        assertEquals("RECHAZADO", r.estado());
        assertEquals("Edad faltante", r.motivo());
    }

    @Test
    void edadFueraDeRangoSeRechaza() {
        var r = intereses.validar(
                new InteresesJobConfig.Linea(
                        5, "103,Bob Johnson,12000,150,prestamo"),
                "semana_3");

        assertEquals("RECHAZADO", r.estado());
    }

    @Test
    void tipoFinancieroDesconocidoSeRechaza() {
        var r = intereses.validar(
                new InteresesJobConfig.Linea(
                        6, "103,Bob Johnson,12000,30,unknown"),
                "semana_3");

        assertEquals("RECHAZADO", r.estado());
    }

    @Test
    void estadoNormalizaFechaConBarras() {
        var r = estados.validar(
                new EstadosJobConfig.Linea(
                        2,
                        "101,2024/10/01,deposito,2500,Ingreso extra"),
                "semana_2");

        assertEquals("VALIDO", r.estado());
        assertEquals(
                LocalDate.of(2024, 10, 1),
                r.fecha());
    }

    @Test
    void estadoNormalizaFechaDiaMesAnio() {
        var r = estados.validar(
                new EstadosJobConfig.Linea(
                        3,
                        "101,15-03-2024,retiro,500,Retiro parcial"),
                "semana_3");

        assertEquals("VALIDO", r.estado());
        assertEquals(
                LocalDate.of(2024, 3, 15),
                r.fecha());
    }

    @Test
    void estadoNormalizaDepositoConTilde() {
        var r = estados.validar(
                new EstadosJobConfig.Linea(
                        4,
                        "101,2024-01-01,depósito,1000,Ingreso mensual"),
                "semana_3");

        assertEquals("VALIDO", r.estado());
        assertEquals("deposito", r.transaccion());
    }

    @Test
    void estadoSinDescripcionSeRechaza() {
        var r = estados.validar(
                new EstadosJobConfig.Linea(
                        5, "101,2024-01-01,deposito,1000,"),
                "semana_3");

        assertEquals("RECHAZADO", r.estado());
        assertEquals("Descripcion vacia", r.motivo());
    }

    @Test
    void estadoConFechaImposibleSeRechaza() {
        var r = estados.validar(
                new EstadosJobConfig.Linea(
                        6,
                        "101,2024-13-01,deposito,1000,Ingreso"),
                "semana_3");

        assertEquals("RECHAZADO", r.estado());
    }
}
