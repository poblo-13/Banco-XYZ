package com.bancoxyz.bff.web.auth;

import com.bancoxyz.bff.web.auth.dto.WebAuthRequest;
import com.bancoxyz.bff.web.auth.dto.WebAuthResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class WebAuthController {

    private final WebAuthService webAuthService;

    public WebAuthController(WebAuthService webAuthService) {
        this.webAuthService = webAuthService;
    }

    @PostMapping("/token")
    public ResponseEntity<WebAuthResponse> obtenerToken(
            @Valid @RequestBody WebAuthRequest request) {

        return ResponseEntity.ok(
                webAuthService.autenticar(request)
        );
    }
}