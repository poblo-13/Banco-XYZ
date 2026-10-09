package com.bancoxyz.customer.service;

import com.bancoxyz.customer.entity.Cliente;
import com.bancoxyz.customer.repository.ClienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Cliente obtenerPorId(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cliente no encontrado: " + clienteId
                ));
    }

    @Transactional
    public Cliente registrar(String nombre, Integer edad, String correo) {
        Cliente cliente = new Cliente(nombre, edad, correo);
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente actualizar(Long clienteId, String nombre,
                              Integer edad, String correo) {
        Cliente cliente = obtenerPorId(clienteId);
        cliente.actualizarPerfil(nombre, edad, correo);
        return clienteRepository.save(cliente);
    }
}