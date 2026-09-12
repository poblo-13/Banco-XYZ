package com.bancoxyz.core.controller;

import com.bancoxyz.core.dto.CuentaResponse;
import com.bancoxyz.core.dto.MovimientoResponse;
import com.bancoxyz.core.mapper.CuentaMapper;
import com.bancoxyz.core.mapper.MovimientoMapper;
import com.bancoxyz.core.service.CuentaService;
import com.bancoxyz.core.service.MovimientoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/core/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;
    private final MovimientoService movimientoService;
    private final CuentaMapper cuentaMapper;
    private final MovimientoMapper movimientoMapper;

    public CuentaController(
            CuentaService cuentaService,
            MovimientoService movimientoService,
            CuentaMapper cuentaMapper,
            MovimientoMapper movimientoMapper) {

        this.cuentaService = cuentaService;
        this.movimientoService = movimientoService;
        this.cuentaMapper = cuentaMapper;
        this.movimientoMapper = movimientoMapper;
    }

    @GetMapping
    public List<CuentaResponse> listarCuentas() {
        return cuentaService.listarTodas()
                .stream()
                .map(cuentaMapper::toResponse)
                .toList();
    }

    @GetMapping("/{cuentaId}")
    public CuentaResponse obtenerCuenta(
            @PathVariable Long cuentaId) {

        return cuentaMapper.toResponse(
                cuentaService.obtenerPorId(cuentaId)
        );
    }

    @GetMapping("/{cuentaId}/movimientos")
    public List<MovimientoResponse> obtenerMovimientos(
            @PathVariable Long cuentaId) {

        cuentaService.obtenerPorId(cuentaId);

        return movimientoService.obtenerPorCuenta(cuentaId)
                .stream()
                .map(movimientoMapper::toResponse)
                .toList();
    }
}