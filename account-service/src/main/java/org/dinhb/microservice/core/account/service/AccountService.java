package org.dinhb.microservice.core.account.service;

import jakarta.transaction.Transactional;
import org.dinhb.microservice.core.account.domain.Account;
import org.dinhb.microservice.core.account.domain.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AccountService {

    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Account create(UUID ownerUserId, Account.AccountType type, String currency) {
        String number = generateAccountNumber();
        return repository.save(new Account(ownerUserId, number, type, currency));
    }

    public List<Account> listByOwner(UUID ownerUserId) {
        return repository.findByOwnerUserId(ownerUserId);
    }

    public Account get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    @Transactional
    public Account debit(UUID id, BigDecimal amount) {
        Account account = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        try {
            account.debit(amount);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        }
        return account;
    }

    @Transactional
    public Account credit(UUID id, BigDecimal amount) {
        Account account = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        account.credit(amount);
        return account;
    }

    private String generateAccountNumber() {
        StringBuilder sb = new StringBuilder("ACC");
        for (int i = 0; i < 13; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        return sb.toString();
    }
}
