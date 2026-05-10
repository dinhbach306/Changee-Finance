package org.dinhb.microservice.core.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "gateway.jwt")
public record GatewayJwtProperties(
        String secret,
        String issuer,
        Duration ttl,
        List<String> publicPaths
) {}
