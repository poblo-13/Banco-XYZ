package com.bancoxyz.account.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "operaciones_cuenta")
public class OperacionCuenta {

    @Id
    @Column(name = "operacion_id", length = 36)
    private String operacionId;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    @Column(name = "cuenta_destino_id")
    private Long cuentaDestinoId;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldo_resultante", nullable = false,
            precision = 15, scale = 2)
    private BigDecimal saldoResultante;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected OperacionCuenta() {
    }

    public OperacionCuenta(String operacionId, Long cuentaId,
                           Long cuentaDestinoId, String tipo,
                           BigDecimal monto, BigDecimal saldoResultante) {
        this.operacionId = operacionId;
        this.cuentaId = cuentaId;
        this.cuentaDestinoId = cuentaDestinoId;
        this.tipo = tipo;
        this.monto = monto;
        this.saldoResultante = saldoResultante;
        this.fecha = LocalDateTime.now();
    }

    public String getOperacionId() { return operacionId; }
    public Long getCuentaId() { return cuentaId; }
    public Long getCuentaDestinoId() { return cuentaDestinoId; }
    public String getTipo() { return tipo; }
    public BigDecimal getMonto() { return monto; }
    public BigDecimal getSaldoResultante() { return saldoResultante; }
    public LocalDateTime getFecha() { return fecha; }
}