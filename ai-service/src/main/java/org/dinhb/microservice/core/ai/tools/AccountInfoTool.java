package org.dinhb.microservice.core.ai.tools;

import org.dinhb.microservice.core.common.security.HeaderNames;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Component
public class AccountInfoTool {

    public static final String USER_ID_KEY = "userId";
    private static final String ACCOUNT_SERVICE_ID = "account-service";

    private final RestClient restClient = RestClient.create();
    private final LoadBalancerClient loadBalancerClient;

    public AccountInfoTool(LoadBalancerClient loadBalancerClient) {
        this.loadBalancerClient = loadBalancerClient;
    }

    @Tool(description = "Get the current balance, currency, type and status of all accounts owned by the user who is currently chatting")
    public List<AccountSummary> getMyAccounts(ToolContext toolContext) {
        UUID userId = resolveUserId(toolContext);
        URI accountsUri = resolveAccountsUri();

        List<AccountResponse> accounts = restClient.get()
                .uri(accountsUri)
                .header(HeaderNames.X_USER_ID, userId.toString())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (accounts == null || accounts.isEmpty()) {
            return List.of();
        }
        return accounts.stream().map(AccountSummary::from).toList();
    }

    private URI resolveAccountsUri() {
        ServiceInstance instance = loadBalancerClient.choose(ACCOUNT_SERVICE_ID);
        if (instance == null) {
            throw new IllegalStateException("No account-service instance available in Eureka");
        }
        return URI.create(instance.getUri() + "/me");
    }

    private static UUID resolveUserId(ToolContext toolContext) {
        Object raw = toolContext.getContext().get(USER_ID_KEY);
        if (raw == null) {
            throw new IllegalStateException("Missing userId in ToolContext — chat must inject the caller's user id");
        }
        return UUID.fromString(raw.toString());
    }

    public record AccountSummary(String accountNumber, String type, BigDecimal balance, String currency, String status) {
        static AccountSummary from(AccountResponse a) {
            return new AccountSummary(a.accountNumber(), a.type(), a.balance(), a.currency(), a.status());
        }
    }

    private record AccountResponse(UUID id, UUID ownerUserId, String accountNumber,
                                   String type, BigDecimal balance, String currency, String status) {}
}
