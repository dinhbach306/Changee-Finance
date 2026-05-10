package org.dinhb.microservice.core.auth.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_credentials", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class AuthCredential {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 255)
    private String roles = "ROLE_CUSTOMER";

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected AuthCredential() {}

    public AuthCredential(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRoles() { return roles; }
    public Instant getCreatedAt() { return createdAt; }
}
