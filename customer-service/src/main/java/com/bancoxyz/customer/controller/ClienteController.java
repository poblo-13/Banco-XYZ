package com.bancoxyz.customer.controller;

import com.bancoxyz.customer.entity.Cliente;
import com.bancoxyz.customer.service.ClienteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public List<ClienteResponse> listar() {
        return clienteService.listarTodos()
                .stream()
                .map(ClienteResponse::from)
                .toList();
    }

    @GetMapping("/{clienteId}")
    public ClienteResponse obtener(@PathVariable Long clienteId) {
        return ClienteResponse.from(
                clienteService.obtenerPorId(clienteId)
        );
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> registrar(
            @Valid @RequestBody ClienteRequest request) {

        Cliente cliente = clienteService.registrar(
                request.nombre(),
                request.edad(),
                request.correo()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ClienteResponse.from(cliente));
    }

    @PutMapping("/{clienteId}")
    public ClienteResponse actualizar(
            @PathVariable Long clienteId,
            @Valid @RequestBody ClienteRequest request) {

        return ClienteResponse.from(
                clienteService.actualizar(
                        clienteId,
                        request.nombre(),
                        request.edad(),
                        request.correo()
                )
        );
    }

    public record ClienteRequest(
            @NotBlank @Size(max = 100) String nombre,
            @Min(0) @Max(120) Integer edad,
            @NotBlank @Email @Size(max = 150) String correo
    ) {
    }

    public record ClienteResponse(
            Long clienteId,
            String nombre,
            Integer edad,
            String correo
    ) {
        public static ClienteResponse from(Cliente cliente) {
            return new ClienteResponse(
                    cliente.getClienteId(),
                    cliente.getNombre(),
                    cliente.getEdad(),
                    cliente.getCorreo()
            );
        }
    }
}