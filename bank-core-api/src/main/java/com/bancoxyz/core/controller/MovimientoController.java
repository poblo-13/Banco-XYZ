package com.bancoxyz.core.controller;

import com.bancoxyz.core.dto.MovimientoRequest;
import com.bancoxyz.core.dto.MovimientoResponse;
import com.bancoxyz.core.entity.Movimiento;
import com.bancoxyz.core.mapper.MovimientoMapper;
import com.bancoxyz.core.service.CuentaService;
import com.bancoxyz.core.service.MovimientoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/core/movimientos")
public class MovimientoController {

    private final MovimientoService movimientoService;
    private final CuentaService cuentaService;
    private final MovimientoMapper movimientoMapper;

    public MovimientoController(
            MovimientoService movimientoService,
            CuentaService cuentaService,
            MovimientoMapper movimientoMapper) {

        this.movimientoService = movimientoService;
        this.cuentaService = cuentaService;
        this.movimientoMapper = movimientoMapper;
    }

    @PostMapping
    public ResponseEntity<MovimientoResponse> crearMovimiento(
            @Valid @RequestBody MovimientoRequest request) {

        cuentaService.obtenerPorId(request.cuentaId());

        Movimiento movimiento =
                movimientoMapper.toEntity(request);

        Movimiento guardado =
                movimientoService.registrarOperacion(movimiento);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(movimientoMapper.toResponse(guardado));
    }
}