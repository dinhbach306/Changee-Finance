package org.dinhb.microservice.core.transaction.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
        @Index(columnList = "initiatorUserId"),
        @Index(columnList = "fromAccountId"),
        @Index(columnList = "toAccountId")
})
public class Transaction {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID initiatorUserId;

    @Column(nullable = false)
    private UUID fromAccountId;

    @Column(nullable = false)
    private UUID toAccountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.PENDING;

    @Column(length = 255)
    private String reason;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant settledAt;

    protected Transaction() {}

    public Transaction(UUID initiatorUserId, UUID fromAccountId, UUID toAccountId,
                       BigDecimal amount, String currency) {
        this.initiatorUserId = initiatorUserId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.currency = currency;
    }

    public void markCompleted() {
        this.status = Status.COMPLETED;
        this.settledAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = Status.FAILED;
        this.reason = reason;
        this.settledAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getInitiatorUserId() { return initiatorUserId; }
    public UUID getFromAccountId() { return fromAccountId; }
    public UUID getToAccountId() { return toAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Status getStatus() { return status; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSettledAt() { return settledAt; }

    public enum Status { PENDING, COMPLETED, FAILED }
}
