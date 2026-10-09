package com.bancoxyz.account.controller;

import com.bancoxyz.account.entity.Cuenta;
import com.bancoxyz.account.service.CuentaService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping
    public List<CuentaResponse> listarCuentas() {
        return cuentaService.listarTodas()
                .stream()
                .map(CuentaResponse::from)
                .toList();
    }

    @GetMapping("/{cuentaId}")
    public CuentaResponse obtenerCuenta(@PathVariable Long cuentaId) {
        return CuentaResponse.from(cuentaService.obtenerPorId(cuentaId));
    }

    public record CuentaResponse(
            Long cuentaId,
            Long clienteId,
            String tipo,
            BigDecimal saldoActual,
            BigDecimal tasaInteres,
            BigDecimal interesCalculado
    ) {
        public static CuentaResponse from(Cuenta cuenta) {
            return new CuentaResponse(
                    cuenta.getCuentaId(),
                    cuenta.getClienteId(),
                    cuenta.getTipo(),
                    cuenta.getSaldoActual(),
                    cuenta.getTasaInteres(),
                    cuenta.getInteresCalculado()
            );
        }
    }
}