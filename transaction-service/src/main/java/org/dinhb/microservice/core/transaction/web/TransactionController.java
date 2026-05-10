package org.dinhb.microservice.core.transaction.web;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.dinhb.microservice.core.common.security.HeaderNames;
import org.dinhb.microservice.core.transaction.domain.Transaction;
import org.dinhb.microservice.core.transaction.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@RateLimiter(name = "transactionServiceRl")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping("/transfer")
    public TransactionResponse transfer(
            @RequestHeader(HeaderNames.X_USER_ID) UUID userId,
            @Valid @RequestBody TransferRequest req) {
        Transaction txn = service.transfer(userId, req.fromAccountId(), req.toAccountId(),
                req.amount(), req.currency() == null ? "USD" : req.currency());
        return TransactionResponse.from(txn);
    }

    @GetMapping("/me")
    public List<TransactionResponse> mine(@RequestHeader(HeaderNames.X_USER_ID) UUID userId) {
        return service.history(userId).stream().map(TransactionResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TransactionResponse byId(@PathVariable UUID id) {
        return TransactionResponse.from(service.get(id));
    }

    public record TransferRequest(
            @NotNull UUID fromAccountId,
            @NotNull UUID toAccountId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            String currency
    ) {}

    public record TransactionResponse(
            UUID id, UUID fromAccountId, UUID toAccountId,
            BigDecimal amount, String currency, String status, String reason,
            Instant createdAt, Instant settledAt
    ) {
        public static TransactionResponse from(Transaction t) {
            return new TransactionResponse(t.getId(), t.getFromAccountId(), t.getToAccountId(),
                    t.getAmount(), t.getCurrency(), t.getStatus().name(), t.getReason(),
                    t.getCreatedAt(), t.getSettledAt());
        }
    }
}
