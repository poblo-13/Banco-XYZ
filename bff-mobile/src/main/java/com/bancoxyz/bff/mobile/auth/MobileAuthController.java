package com.bancoxyz.bff.mobile.auth;

import com.bancoxyz.bff.mobile.auth.dto.MobileAuthRequest;
import com.bancoxyz.bff.mobile.auth.dto.MobileAuthResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class MobileAuthController {

    private final MobileAuthService mobileAuthService;

    public MobileAuthController(
            MobileAuthService mobileAuthService) {

        this.mobileAuthService = mobileAuthService;
    }

    @PostMapping("/token")
    public ResponseEntity<MobileAuthResponse> obtenerToken(
            @Valid @RequestBody MobileAuthRequest request) {

        return ResponseEntity.ok(
                mobileAuthService.autenticar(request)
        );
    }
}