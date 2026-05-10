package org.dinhb.microservice.core.transaction.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class AccountClientFallbackFactory implements FallbackFactory<AccountClient> {

    private static final Logger log = LoggerFactory.getLogger(AccountClientFallbackFactory.class);

    @Override
    public AccountClient create(Throwable cause) {
        return new AccountClient() {
            @Override
            public AccountDto get(UUID id) {
                log.warn("Fallback: account-service unavailable for get({}): {}", id, cause.getMessage());
                throw new AccountServiceUnavailableException(cause);
            }

            @Override
            public AccountDto debit(UUID id, AmountRequest req) {
                log.warn("Fallback: account-service unavailable for debit({}, {}): {}", id, req.amount(), cause.getMessage());
                throw new AccountServiceUnavailableException(cause);
            }

            @Override
            public AccountDto credit(UUID id, AmountRequest req) {
                log.warn("Fallback: account-service unavailable for credit({}, {}): {}", id, req.amount(), cause.getMessage());
                throw new AccountServiceUnavailableException(cause);
            }
        };
    }

    public static class AccountServiceUnavailableException extends RuntimeException {
        public AccountServiceUnavailableException(Throwable cause) {
            super("account-service circuit breaker open", cause);
        }
    }
}
