package com.bancoxyz.bff.atm.auth;

import com.bancoxyz.bff.atm.auth.dto.AtmAuthRequest;
import com.bancoxyz.bff.atm.auth.dto.AtmAuthResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AtmAuthController {

    private final AtmAuthService atmAuthService;

    public AtmAuthController(
            AtmAuthService atmAuthService) {

        this.atmAuthService = atmAuthService;
    }

    @PostMapping("/token")
    public ResponseEntity<AtmAuthResponse> obtenerToken(
            @Valid @RequestBody AtmAuthRequest request) {

        return ResponseEntity.ok(
                atmAuthService.autenticar(request)
        );
    }
}