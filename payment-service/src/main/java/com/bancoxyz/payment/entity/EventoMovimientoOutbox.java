package com.bancoxyz.payment.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "eventos_movimiento_outbox")
public class EventoMovimientoOutbox {

    @Id
    @Column(name = "operacion_id", nullable = false, length = 36)
    private String operacionId;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "estado", nullable = false, length = 15)
    private String estado;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "publicado_en")
    private Instant publicadoEn;

    @Column(name = "intentos", nullable = false)
    private int intentos;

    @Column(name = "ultimo_error", length = 255)
    private String ultimoError;

    @Version
    private Long version;

    protected EventoMovimientoOutbox() {
    }

    public EventoMovimientoOutbox(Movimiento movimiento) {

        this.operacionId = movimiento.getOperacionId();

        // Todos los campos incluidos provienen de valores
        // numericos o identificadores previamente validados.
        this.payload =
                "{\"evento\":\"MOVIMIENTO_CONFIRMADO\","
                + "\"operacionId\":\"" + movimiento.getOperacionId() + "\","
                + "\"cuentaId\":" + movimiento.getCuentaId() + ","
                + "\"tipo\":\"" + movimiento.getTipoMovimiento() + "\","
                + "\"monto\":" + movimiento.getMonto().toPlainString() + ","
                + "\"saldoResultante\":"
                + movimiento.getSaldoResultante().toPlainString()
                + "}";

        this.estado = "PENDIENTE";
        this.creadoEn = Instant.now();
        this.intentos = 0;
    }

    public String getOperacionId() {
        return operacionId;
    }

    public String getPayload() {
        return payload;
    }

    public String getEstado() {
        return estado;
    }

    public int getIntentos() {
        return intentos;
    }

    public void marcarPublicado() {
        this.estado = "PUBLICADO";
        this.publicadoEn = Instant.now();
        this.ultimoError = null;
    }

    public void registrarFallo(String error) {
        this.intentos++;
        this.estado = "PENDIENTE";
        this.ultimoError = error == null
                ? "Error de publicacion"
                : error.substring(0, Math.min(error.length(), 255));
    }
}
