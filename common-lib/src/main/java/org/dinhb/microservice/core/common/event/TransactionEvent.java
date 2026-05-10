package org.dinhb.microservice.core.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionEvent(
        UUID transactionId,
        UUID fromAccountId,
        UUID toAccountId,
        UUID initiatorUserId,
        BigDecimal amount,
        String currency,
        Status status,
        String reason,
        Instant occurredAt
) {
    public enum Status {
        CREATED, COMPLETED, FAILED
    }
}
