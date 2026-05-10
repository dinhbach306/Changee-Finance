package org.dinhb.microservice.core.auth.web;

import jakarta.validation.Valid;
import org.dinhb.microservice.core.auth.service.AuthService;
import org.dinhb.microservice.core.auth.web.dto.LoginRequest;
import org.dinhb.microservice.core.auth.web.dto.SignupRequest;
import org.dinhb.microservice.core.auth.web.dto.TokenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(@Valid @RequestBody SignupRequest req) {
        return ResponseEntity.ok(service.signup(req));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(service.login(req));
    }
}
