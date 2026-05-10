package org.dinhb.microservice.core.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "account-service", fallbackFactory = AccountClientFallbackFactory.class)
public interface AccountClient {

    @GetMapping("/{id}")
    AccountDto get(@PathVariable("id") UUID id);

    @PostMapping("/{id}/debit")
    AccountDto debit(@PathVariable("id") UUID id, @RequestBody AmountRequest req);

    @PostMapping("/{id}/credit")
    AccountDto credit(@PathVariable("id") UUID id, @RequestBody AmountRequest req);

    record AmountRequest(BigDecimal amount) {}

    record AccountDto(UUID id, UUID ownerUserId, String accountNumber,
                     String type, BigDecimal balance, String currency, String status) {}
}
