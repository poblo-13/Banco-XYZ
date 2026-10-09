package com.bancoxyz.payment.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "movimientos",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_movimiento_operacion_id",
        columnNames = "operacion_id"
    )
)
public class Movimiento {

    public enum Estado {
        PENDIENTE,
        CONFIRMADO,
        RECHAZADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operacion_id", nullable = false, length = 36)
    private String operacionId;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "tipo_movimiento", nullable = false, length = 30)
    private String tipoMovimiento;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(length = 255)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado;

    @Column(name = "saldo_resultante", precision = 15, scale = 2)
    private BigDecimal saldoResultante;

    protected Movimiento() {
    }

    public Movimiento(
            String operacionId,
            Long cuentaId,
            String tipoMovimiento,
            BigDecimal monto,
            String descripcion) {

        this.operacionId = operacionId;
        this.cuentaId = cuentaId;
        this.fecha = LocalDate.now();
        this.tipoMovimiento = tipoMovimiento;
        this.monto = monto;
        this.descripcion = descripcion;
        this.estado = Estado.PENDIENTE;
    }

    public void confirmar(BigDecimal saldoResultante) {
        this.saldoResultante = saldoResultante;
        this.estado = Estado.CONFIRMADO;
    }

    public void rechazar() {
        this.estado = Estado.RECHAZADO;
    }

    public Long getId() {
        return id;
    }

    public String getOperacionId() {
        return operacionId;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getTipoMovimiento() {
        return tipoMovimiento;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Estado getEstado() {
        return estado;
    }

    public BigDecimal getSaldoResultante() {
        return saldoResultante;
    }
}