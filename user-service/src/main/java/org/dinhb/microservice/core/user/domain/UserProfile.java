package org.dinhb.microservice.core.user.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    private UUID userId;

    @Column(nullable = false, length = 128)
    private String fullName;

    @Column(length = 32)
    private String phone;

    @Column(length = 255)
    private String address;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private KycStatus kycStatus = KycStatus.PENDING;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected UserProfile() {}

    public UserProfile(UUID userId, String fullName) {
        this.userId = userId;
        this.fullName = fullName;
    }

    public void update(String fullName, String phone, String address, LocalDate dateOfBirth) {
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.updatedAt = Instant.now();
    }

    public void approveKyc() {
        this.kycStatus = KycStatus.APPROVED;
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public KycStatus getKycStatus() { return kycStatus; }
    public Instant getUpdatedAt() { return updatedAt; }

    public enum KycStatus { PENDING, APPROVED, REJECTED }
}
