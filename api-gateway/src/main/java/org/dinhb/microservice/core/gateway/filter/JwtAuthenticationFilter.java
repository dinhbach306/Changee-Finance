package org.dinhb.microservice.core.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.dinhb.microservice.core.common.security.HeaderNames;
import org.dinhb.microservice.core.common.security.JwtUtils;
import org.dinhb.microservice.core.gateway.config.GatewayJwtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER = "Bearer ";

    private final JwtUtils jwtUtils;
    private final List<String> publicPaths;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtUtils jwtUtils, GatewayJwtProperties props) {
        this.jwtUtils = jwtUtils;
        this.publicPaths = props.publicPaths() == null ? List.of() : props.publicPaths();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            return unauthorized(exchange, "Missing bearer token");
        }
        String token = header.substring(BEARER.length()).trim();

        try {
            Claims claims = jwtUtils.parse(token);
            JwtUtils.UserContext ctx = jwtUtils.toContext(claims);
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header(HeaderNames.X_USER_ID, ctx.userId().toString())
                    .header(HeaderNames.X_USERNAME, ctx.username())
                    .header(HeaderNames.X_USER_ROLES, String.join(",", ctx.roles()))
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (JwtException ex) {
            log.warn("JWT validation failed for {}: {}", path, ex.getMessage());
            return unauthorized(exchange, "Invalid token");
        }
    }

    private boolean isPublic(String path) {
        return publicPaths.stream().anyMatch(p -> matcher.match(p, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Auth-Error", reason);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
