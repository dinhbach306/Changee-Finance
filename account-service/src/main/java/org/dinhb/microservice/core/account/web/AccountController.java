package org.dinhb.microservice.core.account.web;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.dinhb.microservice.core.account.domain.Account;
import org.dinhb.microservice.core.account.service.AccountService;
import org.dinhb.microservice.core.common.security.HeaderNames;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
@RateLimiter(name = "accountServiceRl")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    public AccountResponse create(
            @RequestHeader(HeaderNames.X_USER_ID) UUID userId,
            @Valid @RequestBody CreateAccountRequest req) {
        Account a = service.create(userId, req.type(), req.currency() == null ? "USD" : req.currency());
        return AccountResponse.from(a);
    }

    @GetMapping("/me")
    public List<AccountResponse> mine(@RequestHeader(HeaderNames.X_USER_ID) UUID userId) {
        return service.listByOwner(userId).stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AccountResponse byId(@PathVariable UUID id) {
        return AccountResponse.from(service.get(id));
    }

    @PostMapping("/{id}/debit")
    public AccountResponse debit(@PathVariable UUID id, @Valid @RequestBody AmountRequest req) {
        return AccountResponse.from(service.debit(id, req.amount()));
    }

    @PostMapping("/{id}/credit")
    public AccountResponse credit(@PathVariable UUID id, @Valid @RequestBody AmountRequest req) {
        return AccountResponse.from(service.credit(id, req.amount()));
    }

    public record CreateAccountRequest(@NotNull Account.AccountType type, String currency) {}

    public record AmountRequest(@NotNull @DecimalMin("0.01") BigDecimal amount) {}

    public record AccountResponse(UUID id, UUID ownerUserId, String accountNumber,
                                  String type, BigDecimal balance, String currency, String status) {
        public static AccountResponse from(Account a) {
            return new AccountResponse(a.getId(), a.getOwnerUserId(), a.getAccountNumber(),
                    a.getType().name(), a.getBalance(), a.getCurrency(), a.getStatus().name());
        }
    }
}
