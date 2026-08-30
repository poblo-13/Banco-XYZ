package com.bancoxyz.batch.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

import jakarta.persistence.Column;

@Entity
@Table(name = "intereses_calculados")
public class InteresCalculado {

    @Id
    private Long cuentaId;

    private String nombre;
    private BigDecimal saldoInicial;
    private Integer edad;
    private String tipo;
    @Column(precision = 6, scale = 4)
    private BigDecimal tasaInteres;
    private BigDecimal interesCalculado;
    private BigDecimal saldoFinal;

    public InteresCalculado() {
    }

    public InteresCalculado(
            Long cuentaId,
            String nombre,
            BigDecimal saldoInicial,
            Integer edad,
            String tipo,
            BigDecimal tasaInteres,
            BigDecimal interesCalculado,
            BigDecimal saldoFinal) {

        this.cuentaId = cuentaId;
        this.nombre = nombre;
        this.saldoInicial = saldoInicial;
        this.edad = edad;
        this.tipo = tipo;
        this.tasaInteres = tasaInteres;
        this.interesCalculado = interesCalculado;
        this.saldoFinal = saldoFinal;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public Integer getEdad() {
        return edad;
    }

    public void setEdad(Integer edad) {
        this.edad = edad;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getTasaInteres() {
        return tasaInteres;
    }

    public void setTasaInteres(BigDecimal tasaInteres) {
        this.tasaInteres = tasaInteres;
    }

    public BigDecimal getInteresCalculado() {
        return interesCalculado;
    }

    public void setInteresCalculado(BigDecimal interesCalculado) {
        this.interesCalculado = interesCalculado;
    }

    public BigDecimal getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(BigDecimal saldoFinal) {
        this.saldoFinal = saldoFinal;
    }
}