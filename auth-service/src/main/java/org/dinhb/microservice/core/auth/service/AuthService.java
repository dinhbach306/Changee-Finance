package org.dinhb.microservice.core.auth.service;

import jakarta.transaction.Transactional;
import org.dinhb.microservice.core.auth.domain.AuthCredential;
import org.dinhb.microservice.core.auth.domain.AuthCredentialRepository;
import org.dinhb.microservice.core.auth.web.dto.LoginRequest;
import org.dinhb.microservice.core.auth.web.dto.SignupRequest;
import org.dinhb.microservice.core.auth.web.dto.TokenResponse;
import org.dinhb.microservice.core.common.security.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

@Service
public class AuthService {

    private final AuthCredentialRepository repository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwt;

    public AuthService(AuthCredentialRepository repository, PasswordEncoder encoder, JwtUtils jwt) {
        this.repository = repository;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public TokenResponse signup(SignupRequest req) {
        if (repository.existsByUsername(req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username taken");
        }
        AuthCredential cred = new AuthCredential(req.username(), encoder.encode(req.password()));
        repository.save(cred);
        return issue(cred);
    }

    public TokenResponse login(LoginRequest req) {
        AuthCredential cred = repository.findByUsername(req.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!encoder.matches(req.password(), cred.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return issue(cred);
    }

    private TokenResponse issue(AuthCredential cred) {
        List<String> roles = Arrays.stream(cred.getRoles().split(",")).map(String::trim).toList();
        String token = jwt.generate(cred.getId(), cred.getUsername(), roles);
        return new TokenResponse(token, cred.getId(), cred.getUsername(), roles);
    }
}
