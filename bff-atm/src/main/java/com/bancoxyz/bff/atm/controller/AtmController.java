package com.bancoxyz.bff.atm.controller;

import com.bancoxyz.bff.atm.dto.AtmRetiroRequest;
import com.bancoxyz.bff.atm.dto.AtmRetiroResponse;
import com.bancoxyz.bff.atm.dto.AtmSaldoResponse;
import com.bancoxyz.bff.atm.service.AtmService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atm")
public class AtmController {

    private final AtmService atmService;

    public AtmController(AtmService atmService) {
        this.atmService = atmService;
    }

    @GetMapping("/saldo/{cuentaId}")
    public ResponseEntity<AtmSaldoResponse> consultarSaldo(
            @PathVariable Long cuentaId) {

        return ResponseEntity.ok(
                atmService.consultarSaldo(cuentaId)
        );
    }

    @PostMapping("/retiros")
    public ResponseEntity<AtmRetiroResponse> retirar(
            @Valid @RequestBody AtmRetiroRequest request) {

        return ResponseEntity.ok(
                atmService.retirar(request)
        );
    }
}