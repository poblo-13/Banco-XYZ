package com.bancoxyz.customer.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(nullable = false, length = 100)
    private String nombre;

    private Integer edad;

    @Column(length = 150, unique = true)
    private String correo;

    public Cliente() {
    }

    public Cliente(String nombre, Integer edad, String correo) {
        this.nombre = nombre;
        this.edad = edad;
        this.correo = correo;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public String getNombre() {
        return nombre;
    }

    public Integer getEdad() {
        return edad;
    }

    public String getCorreo() {
        return correo;
    }

    public void actualizarPerfil(String nombre, Integer edad, String correo) {
        this.nombre = nombre;
        this.edad = edad;
        this.correo = correo;
    }
}