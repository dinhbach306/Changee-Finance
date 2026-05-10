package org.dinhb.microservice.core.gateway.config;

import org.dinhb.microservice.core.common.security.HeaderNames;
import org.dinhb.microservice.core.common.security.JwtUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
@EnableConfigurationProperties(GatewayJwtProperties.class)
public class GatewayConfig {

    @Bean
    public JwtUtils jwtUtils(GatewayJwtProperties props) {
        return new JwtUtils(props.secret(), props.issuer(), props.ttl());
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.justOrEmpty(
                exchange.getRequest().getHeaders().getFirst(HeaderNames.X_USER_ID)
        ).switchIfEmpty(Mono.fromSupplier(() ->
                exchange.getRequest().getRemoteAddress() == null
                        ? "anonymous"
                        : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        ));
    }
}
