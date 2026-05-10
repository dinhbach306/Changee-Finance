package org.dinhb.microservice.core.transaction.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import org.dinhb.microservice.core.common.event.TransactionEvent;
import org.dinhb.microservice.core.transaction.client.AccountClient;
import org.dinhb.microservice.core.transaction.domain.Transaction;
import org.dinhb.microservice.core.transaction.domain.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final String CB_ACCOUNT = "accountService";

    private final TransactionRepository repository;
    private final AccountClient accountClient;
    private final TransactionEventPublisher publisher;

    public TransactionService(TransactionRepository repository,
                              AccountClient accountClient,
                              TransactionEventPublisher publisher) {
        this.repository = repository;
        this.accountClient = accountClient;
        this.publisher = publisher;
    }

    @Transactional
    public Transaction transfer(UUID initiatorUserId, UUID fromAccountId, UUID toAccountId,
                                BigDecimal amount, String currency) {
        Transaction txn = repository.save(
                new Transaction(initiatorUserId, fromAccountId, toAccountId, amount, currency));
        publisher.publish(toEvent(txn, TransactionEvent.Status.CREATED, null));

        try {
            debitWithCb(fromAccountId, amount);
            try {
                creditWithCb(toAccountId, amount);
            } catch (RuntimeException creditEx) {
                log.error("Credit failed, compensating debit on {}", fromAccountId, creditEx);
                safeCompensate(fromAccountId, amount);
                throw creditEx;
            }
            txn.markCompleted();
            publisher.publish(toEvent(txn, TransactionEvent.Status.COMPLETED, null));
            return txn;
        } catch (RuntimeException ex) {
            txn.markFailed(ex.getMessage());
            publisher.publish(toEvent(txn, TransactionEvent.Status.FAILED, ex.getMessage()));
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Transfer failed: " + ex.getMessage(), ex);
        }
    }

    @CircuitBreaker(name = CB_ACCOUNT)
    @Retry(name = CB_ACCOUNT)
    public AccountClient.AccountDto debitWithCb(UUID accountId, BigDecimal amount) {
        return accountClient.debit(accountId, new AccountClient.AmountRequest(amount));
    }

    @CircuitBreaker(name = CB_ACCOUNT)
    @Retry(name = CB_ACCOUNT)
    public AccountClient.AccountDto creditWithCb(UUID accountId, BigDecimal amount) {
        return accountClient.credit(accountId, new AccountClient.AmountRequest(amount));
    }

    private void safeCompensate(UUID accountId, BigDecimal amount) {
        try {
            accountClient.credit(accountId, new AccountClient.AmountRequest(amount));
        } catch (RuntimeException ex) {
            log.error("Compensation credit failed on {}; manual intervention required", accountId, ex);
        }
    }

    public List<Transaction> history(UUID userId) {
        return repository.findByInitiatorUserIdOrderByCreatedAtDesc(userId);
    }

    public Transaction get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    private TransactionEvent toEvent(Transaction t, TransactionEvent.Status status, String reason) {
        return new TransactionEvent(
                t.getId(), t.getFromAccountId(), t.getToAccountId(), t.getInitiatorUserId(),
                t.getAmount(), t.getCurrency(), status, reason, Instant.now());
    }
}
