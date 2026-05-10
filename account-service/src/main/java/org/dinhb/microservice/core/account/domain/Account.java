package org.dinhb.microservice.core.account.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts", indexes = @Index(columnList = "ownerUserId"))
public class Account {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false, unique = true, length = 32)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.ACTIVE;

    @Version
    private long version;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Account() {}

    public Account(UUID ownerUserId, String accountNumber, AccountType type, String currency) {
        this.ownerUserId = ownerUserId;
        this.accountNumber = accountNumber;
        this.type = type;
        this.currency = currency;
    }

    public void debit(BigDecimal amount) {
        if (status != Status.ACTIVE) throw new IllegalStateException("Account not active");
        if (balance.compareTo(amount) < 0) throw new IllegalStateException("Insufficient funds");
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        if (status != Status.ACTIVE) throw new IllegalStateException("Account not active");
        this.balance = this.balance.add(amount);
    }

    public UUID getId() { return id; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public String getAccountNumber() { return accountNumber; }
    public AccountType getType() { return type; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public enum AccountType { CHECKING, SAVINGS }
    public enum Status { ACTIVE, FROZEN, CLOSED }
}
